import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.StartCommandResult
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Serializable
private data class ScriptRequest(val carpeta: String, val archivo: String)

class DesktopPhotoServerManager(private val state: PhotoServerState) {
    private var apiServer: HttpServer? = null
    private var healthExecutor: ScheduledExecutorService? = null
    private val serverExecutor = Executors.newFixedThreadPool(4)
    private val logFile: Path = Path.of(System.getProperty("user.home"), ".moonlight", "photo-server.log")
    
    private val scriptsDir = "C:\\Users\\gerar\\Desktop\\mi-streamdeck\\scripts"
    private val immichPort = 2283

    init {
        startApiListener()
        startHealthMonitor()
    }

    @Synchronized
    fun start(): StartCommandResult {
        state.updateStatus(PhotoServerStatus.Starting)
        appendLog("Triggering Immich start script...")
        
        return runCatching {
            val scriptPath = "$scriptsDir\\07_Personalizacion\\fotos.bat"
            val process = ProcessBuilder("cmd.exe", "/c", "\"$scriptPath\"")
                .directory(File("$scriptsDir\\07_Personalizacion"))
                .redirectErrorStream(true)
                .start()
            
            // Read output in background to avoid blocking
            serverExecutor.execute {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        appendLog("[Script] $line")
                    }
                }
            }

            // Start monitoring health immediately
            startHealthMonitor()
            
            // Start the API listener if not running
            startApiListener()

            state.lastCommandResult = StartCommandResult.Success
            StartCommandResult.Success
        }.getOrElse {
            val msg = it.message ?: "Error al lanzar script"
            state.updateStatus(PhotoServerStatus.Error(msg))
            state.lastCommandResult = StartCommandResult.Failed(msg)
            appendLog("Start failed: $msg")
            StartCommandResult.Failed(msg)
        }
    }

    @Synchronized
    fun stop() {
        appendLog("Triggering Immich stop script...")
        runCatching {
            val scriptPath = "$scriptsDir\\07_Personalizacion\\cerrar_fotos.bat"
            ProcessBuilder("cmd.exe", "/c", "\"$scriptPath\"")
                .directory(File("$scriptsDir\\07_Personalizacion"))
                .start()
        }
        healthExecutor?.shutdownNow()
        healthExecutor = null
        state.healthMessage = "Detenido"
        state.updateStatus(PhotoServerStatus.Stopped)
    }

    fun restart(): StartCommandResult {
        stop()
        Thread.sleep(2000)
        return start()
    }

    /**
     * Starts an HTTP server on port 3000 to listen for requests from the mobile app.
     */
    private fun startApiListener() {
        if (apiServer != null) return
        
        try {
            val server = HttpServer.create(InetSocketAddress(3000), 0)
            server.createContext("/api/run-script") { exchange ->
                if (exchange.requestMethod == "POST") {
                    val auth = exchange.requestHeaders.getFirst("Authorization")
                    if (auth == "Bearer CasaGerard") {
                        val body = exchange.requestBody.bufferedReader().readText()
                        val req = Json.decodeFromString<ScriptRequest>(body)
                        
                        appendLog("Remote request received: ${req.carpeta}/${req.archivo}")
                        
                        if (req.archivo == "fotos.bat") {
                            start()
                        } else if (req.archivo == "cerrar_fotos.bat") {
                            stop()
                        }
                        
                        val response = "OK"
                        exchange.sendResponseHeaders(200, response.length.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }
                    } else {
                        exchange.sendResponseHeaders(401, 0)
                    }
                } else {
                    exchange.sendResponseHeaders(405, 0)
                }
                exchange.close()
            }
            server.executor = serverExecutor
            server.start()
            apiServer = server
            appendLog("API listener started on port 3000")
        } catch (e: Exception) {
            appendLog("Failed to start API listener: ${e.message}")
        }
    }

    private fun startHealthMonitor() {
        healthExecutor?.shutdownNow()
        healthExecutor = Executors.newSingleThreadScheduledExecutor().also { exec ->
            exec.scheduleAtFixedRate({
                runCatching {
                    val url = URL("http://localhost:$immichPort/api/server/ping")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    val code = conn.responseCode
                    if (code == 200) {
                        val host = InetAddress.getLocalHost().hostAddress ?: "127.0.0.1"
                        val urlText = "http://$host:$immichPort"
                        state.updateConnection(baseUrl = urlText, apiKey = state.connectionConfig.apiKey)
                        state.updateStatus(PhotoServerStatus.Running(immichPort, urlText))
                        state.healthMessage = "Online - ${Instant.now()}"
                    } else {
                        state.healthMessage = "Status $code - ${Instant.now()}"
                    }
                }.onFailure {
                    state.healthMessage = "Offline - ${it.message}"
                    if (state.status is PhotoServerStatus.Running) {
                        state.updateStatus(PhotoServerStatus.Starting)
                    }
                }
            }, 0, 10, TimeUnit.SECONDS)
        }
    }

    fun registerWindowsStartupTask(taskName: String = "MoonlightPhotoServer", runHeadless: Boolean = true): StartCommandResult {
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("win")) return StartCommandResult.Failed("Task Scheduler sólo está disponible en Windows")

        val executable = ProcessHandle.current().info().command().orElse(null)
            ?: return StartCommandResult.Failed("No se pudo detectar el ejecutable actual")
        val argument = if (runHeadless) "--server" else ""
        val command = listOf(
            "schtasks", "/Create", "/TN", taskName, "/SC", "ONLOGON", "/RL", "HIGHEST",
            "/TR", "\"$executable\" $argument", "/F"
        )
        return runCatching {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val ok = process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0
            if (!ok) error("No se pudo registrar tarea: $output")
            appendLog("Windows startup task registered: $taskName")
            StartCommandResult.Success
        }.getOrElse {
            val msg = it.message ?: "Error creando tarea programada"
            appendLog("Startup task registration failed: $msg")
            StartCommandResult.Failed(msg)
        }
    }

    private fun appendLog(message: String) {
        val line = "${Instant.now()} $message"
        runCatching {
            Files.createDirectories(logFile.parent)
            Files.writeString(logFile, "$line\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)
        }
        state.recentLogs = (state.recentLogs + line).takeLast(50)
    }
}

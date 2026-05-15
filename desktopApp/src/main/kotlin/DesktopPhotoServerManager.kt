import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.StartCommandResult
import com.sun.net.httpserver.HttpServer
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

class DesktopPhotoServerManager(private val state: PhotoServerState) {
    private var server: HttpServer? = null
    private var healthExecutor: ScheduledExecutorService? = null
    private val serverExecutor = Executors.newSingleThreadExecutor()
    private val logFile: Path = Path.of(System.getProperty("user.home"), ".moonlight", "photo-server.log")

    @Synchronized
    fun start(): StartCommandResult {
        if (server != null) {
            state.lastCommandResult = StartCommandResult.Success
            return StartCommandResult.Success
        }

        state.updateStatus(PhotoServerStatus.Starting)
        return runCatching {
            Files.createDirectories(logFile.parent)
            appendLog("Starting photo server")
            val httpServer = HttpServer.create(InetSocketAddress(0), 0)
            httpServer.createContext("/") { exchange ->
                val response = "Moonlight Photo Server OK"
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            httpServer.createContext("/health") { exchange ->
                val response = "ok"
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            httpServer.executor = serverExecutor
            httpServer.start()
            server = httpServer

            val host = InetAddress.getLocalHost().hostAddress ?: "127.0.0.1"
            val url = "http://$host:${httpServer.address.port}/"
            state.updateStatus(PhotoServerStatus.Running(httpServer.address.port, url))
            state.lastCommandResult = StartCommandResult.Success
            state.healthMessage = "Iniciado en ${Instant.now()}"
            appendLog("Server running at $url")
            startHealthMonitor()
            StartCommandResult.Success
        }.getOrElse {
            val message = it.message ?: "No se pudo iniciar el servidor"
            state.updateStatus(PhotoServerStatus.Error(message))
            state.lastCommandResult = StartCommandResult.Failed(message)
            appendLog("Start failed: $message")
            StartCommandResult.Failed(message)
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stop(0) }
        server = null
        healthExecutor?.shutdownNow()
        healthExecutor = null
        state.healthMessage = "Detenido"
        appendLog("Server stopped")
        state.updateStatus(PhotoServerStatus.Stopped)
    }

    fun restart(): StartCommandResult {
        stop()
        return start()
    }

    fun registerWindowsStartupTask(taskName: String = "MoonlightPhotoServer", runHeadless: Boolean = true): StartCommandResult {
        if (!isWindows()) return StartCommandResult.Failed("Task Scheduler sólo está disponible en Windows")

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

    private fun startHealthMonitor() {
        val running = state.status as? PhotoServerStatus.Running ?: return
        healthExecutor?.shutdownNow()
        healthExecutor = Executors.newSingleThreadScheduledExecutor().also { exec ->
            exec.scheduleAtFixedRate({
                val message = runCatching {
                    val conn = URL("${running.url}health").openConnection() as HttpURLConnection
                    conn.connectTimeout = 1500
                    conn.readTimeout = 1500
                    conn.requestMethod = "GET"
                    val code = conn.responseCode
                    if (code == 200) "OK ${Instant.now()}" else "FAILED ($code) ${Instant.now()}"
                }.getOrElse { "FAILED (${it.message}) ${Instant.now()}" }
                state.healthMessage = message
                appendLog("Healthcheck: $message")
            }, 0, 15, TimeUnit.SECONDS)
        }
    }

    private fun appendLog(message: String) {
        val line = "${Instant.now()} $message"
        runCatching {
            Files.createDirectories(logFile.parent)
            Files.writeString(logFile, "$line\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)
        }
        state.recentLogs = (state.recentLogs + line).takeLast(100)
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)
}

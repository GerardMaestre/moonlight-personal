import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class DesktopPhotoServerManager(private val state: PhotoServerState) {
    private var server: HttpServer? = null

    @Synchronized
    fun start() {
        if (server != null) return
        state.updateStatus(PhotoServerStatus.Starting)
        runCatching {
            val httpServer = HttpServer.create(InetSocketAddress(0), 0)
            httpServer.createContext("/") { exchange ->
                val response = "Moonlight Photo Server OK"
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            httpServer.executor = Executors.newSingleThreadExecutor()
            httpServer.start()
            server = httpServer
            val host = InetAddress.getLocalHost().hostAddress ?: "127.0.0.1"
            state.updateStatus(PhotoServerStatus.Running(httpServer.address.port, "http://$host:${httpServer.address.port}/"))
        }.onFailure {
            state.updateStatus(PhotoServerStatus.Error(it.message ?: "No se pudo iniciar el servidor"))
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stop(0) }
        server = null
        state.updateStatus(PhotoServerStatus.Stopped)
    }

    fun restart() {
        stop()
        start()
    }
}

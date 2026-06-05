import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress

/**
 * Serves the test fixture over HTTP on a random loopback port.
 *
 * Using HTTP (rather than a `file://` URL) is required so both engines load the
 * page identically: Selenide runs the browser through a BrowserUp proxy, and
 * Chrome will not load `file://` documents — nor grant them `localStorage` —
 * under a manual proxy. It also matches the real target of the framework: web
 * apps served over HTTP.
 */
object FixtureServer {
    private val html: ByteArray = File("src/test/resources/fixture.html").readBytes()
    private val server: HttpServer = createServer()

    val url: String get() = "http://127.0.0.1:${server.address.port}/"

    private fun createServer(): HttpServer {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, html.size.toLong())
            exchange.responseBody.use { it.write(html) }
        }
        s.start()
        Runtime.getRuntime().addShutdownHook(Thread { s.stop(0) })
        return s
    }
}

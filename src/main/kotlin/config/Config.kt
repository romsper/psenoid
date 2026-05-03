package config

import java.time.Duration

enum class EngineType { SELENIDE, PLAYWRIGHT }
enum class ConnectionType { LOCAL, REMOTE }

object PsenoidConfig {
    var engine: EngineType = EngineType.SELENIDE
    var connection: ConnectionType = ConnectionType.LOCAL
    var remoteUrl: String = "http://localhost:4444/wd/hub"
    var wsEndpoint: String = "ws://localhost:3000"
    var browser: String = "chrome"
    var headless: Boolean = false
    var timeout: Duration = Duration.ofSeconds(4)
}
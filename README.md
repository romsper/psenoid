# Psenoid

A framework-agnostic Kotlin wrapper for browser automation that provides a unified API over **Playwright** and **Selenide**, with built-in smart waits and network interception.

## Features

- Single API for both Playwright and Selenide — switch engines via config, no code changes
- Smart waits with polling on every element action (click, type, getText, visibility checks)
- Network mocking and traffic capture (requests & responses)
- Thread-safe engine management via `ThreadLocal` for parallel test execution
- Remote browser support (Selenium Grid / Moon / Playwright WebSocket endpoint)

## Quick Start

```kotlin
import config.EngineType
import config.PsenoidConfig

// Configure once (e.g. in a @BeforeAll or test setup)
PsenoidConfig.engine = EngineType.PLAYWRIGHT   // or EngineType.SELENIDE
PsenoidConfig.headless = true

// Use the global DSL
open("https://example.com")

element("h1").wait(visible)
element("h1").wait(text("Example Domain"))
element("a").click()

// Clean up after each test
Psenoid.close()
```

## Configuration

All settings live in `PsenoidConfig`:

| Property | Default | Description |
|---|---|---|
| `engine` | `SELENIDE` | Browser engine (`SELENIDE` or `PLAYWRIGHT`) |
| `connection` | `LOCAL` | Connection type (`LOCAL` or `REMOTE`) |
| `remoteUrl` | `http://localhost:4444/wd/hub` | Selenium Grid URL (Selenide remote) |
| `wsEndpoint` | `ws://localhost:3000` | Playwright WebSocket endpoint |
| `browser` | `chrome` | Browser name (Selenide only) |
| `headless` | `false` | Run headless |
| `timeout` | `4s` | Smart wait timeout for element actions |

## Element API

```kotlin
// Locate by CSS selector or Selenium By
element("button.submit").click()
element(By.id("username")).type("admin")
element("h1").getText()

// Custom timeout for a single action
element(".slow-element").withTimeout(Duration.ofSeconds(10)).wait(visible)

// Conditions
element(".toast").wait(visible)
element(".title").wait(text("Welcome"))
```

## Network Interception

```kotlin
// Mock a request
mockRequest("**/api/users", MockResponse(
    status = 200,
    body = """{"users": []}""",
    contentType = "application/json"
))

// Capture a request triggered by an action
val req = waitForRequest("/api/login") {
    element("button[type=submit]").click()
}
println(req.body)

// Capture a response
val resp = waitForResponse("/api/data") {
    open("https://example.com/dashboard")
}
println(resp.status)

// Remove all mocks
clearMocks()
```

## JUnit Example

```kotlin
class MyTest {

    @AfterEach
    fun tearDown() = Psenoid.close()

    @Test
    fun `login flow`() {
        PsenoidConfig.engine = EngineType.PLAYWRIGHT
        PsenoidConfig.headless = true

        open("https://myapp.com/login")
        element("#username").type("admin")
        element("#password").type("secret")

        val resp = waitForResponse("/api/session") {
            element("button[type=submit]").click()
        }

        assert(resp.status == 200)
        element(".dashboard").wait(visible)
    }
}
```

## Build

```bash
./gradlew build
```

## License

MIT License - see [LICENSE](LICENSE).
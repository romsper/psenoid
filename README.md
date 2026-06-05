# Psenoid

A framework-agnostic Kotlin wrapper for browser automation that provides a unified API over **Playwright** and **Selenide**, with built-in smart waits and network interception.

## Features

- Single API for both Playwright and Selenide — switch engines via config, no code changes
- Smart waits with polling on every element action (click, type, getText, visibility checks)
- Element collections with size/text wait-conditions
- Network mocking and traffic capture (requests & responses)
- Step logging with automatic screenshot-on-failure via pluggable listeners
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

// Interactions: click, doubleClick, rightClick, hover, type, clear, check/uncheck,
// selectByValue/Text/Index, dragTo, uploadFile, pressKey, scrollTo, focus …
element("#agree").check()
element("#country").selectByText("Norway")
element(".card").hover()

// Custom timeout for a single action
element(".slow-element").withTimeout(Duration.ofSeconds(10)).wait(visible)

// Conditions (smart-waited)
element(".toast").wait(visible)
element(".title").wait(text("Welcome"))
element("#submit").wait(enabled)
element(".badge").wait(hasClass("active"))
```

Available conditions: `visible`, `invisible`, `enabled`, `disabled`, `checked`,
`unchecked`, `exists`, `text`, `textExact`, `value`, `attribute`, `cssValue`, `hasClass`.

## Element Collections

`elements(...)` returns a lazy, live collection — the equivalent of Selenide's `$$`.
Every item is a full element (with smart waits), so you can act on it directly.

```kotlin
val rows = elements(".row")            // or elements(By.cssSelector(".row"))

rows.size()                            // current match count
rows.isEmpty(); rows.isNotEmpty()
rows[2].click()                        // indexed item — fully interactive
rows.first().getText()
rows.last().wait(visible)
rows.texts()                           // ["Alice", "Bob", …] in document order
rows.forEach { it.scrollTo() }         // Iterable: forEach / map / filter

// Smart-waited collection conditions (polled until they hold or timeout)
elements(".item").wait(size(3))
elements(".item").wait(exactTexts("one", "two", "three"))
elements(".item").wait(texts("on", "tw"))        // partial match, in order
elements(".result").wait(sizeGreaterThan(0))
elements(".toast").wait(empty)

// Shortcuts
elements(".item").shouldHaveSize(3)
elements(".toast").shouldBeEmpty()
```

Available collection conditions: `size`, `sizeGreaterThan`, `sizeGreaterThanOrEqual`,
`sizeLessThan`, `sizeLessThanOrEqual`, `empty`, `texts`, `exactTexts`.

## Browser Control

```kotlin
// Navigation
back(); forward(); refresh(); getUrl(); getTitle()

// Frames
switchToFrame("#editor"); switchToDefaultContent()

// Tabs / windows
openNewTab(); switchToTab(1); closeCurrentTab(); getTabCount()

// Cookies & storage
addCookie("token", "abc"); getCookie("token"); clearCookies()
setLocalStorage("k", "v"); getLocalStorage("k"); clearLocalStorage()
setSessionStorage("k", "v"); clearSessionStorage()

// Misc
scrollBy(0, 500); dragAndDrop(".source", ".target"); screenshot()
executeScript("return document.title")   // raw JS (engine-native syntax)
```

> Dialogs — `acceptAlert()`, `dismissAlert()`, `setAlertText()`, `getAlertText()` — work on
> both engines, but the call ordering differs: with Playwright set the policy *before* the
> action that opens the dialog; with Selenide act on the *already-open* alert.

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

## Logging & Reporting

Every element action and wait runs through `Scribe`, which notifies registered listeners
and captures a screenshot when a step fails. Enable the built-in console listener, or plug
in your own (e.g. to forward steps and screenshots to Allure).

```kotlin
import logging.ScribeListener

// Built-in: logs each step; saves failure screenshots to build/psenoid/screenshots
enableConsoleLogging()

// Or register a custom listener
addListener(object : ScribeListener {
    override fun beforeStep(name: String) {}
    override fun afterStep(name: String) {}
    override fun onFail(name: String, error: Throwable, screenshot: ByteArray?) {
        // attach `screenshot` to your report
    }
})

clearListeners()
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
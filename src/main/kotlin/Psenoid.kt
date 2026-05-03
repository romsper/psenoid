import config.EngineType
import config.PsenoidConfig
import elements.Conditions
import elements.WebElement
import engine.BrowserEngine
import engine.PlaywrightStrategy
import engine.SelenideStrategy
import network.MockResponse
import org.openqa.selenium.By

object Psenoid {
    private val threadLocalEngine = ThreadLocal<BrowserEngine>()

    val engine: BrowserEngine
        get() {
            if (threadLocalEngine.get() == null) {
                val strategy = when (PsenoidConfig.engine) {
                    EngineType.PLAYWRIGHT -> PlaywrightStrategy()
                    EngineType.SELENIDE -> SelenideStrategy()
                }
                threadLocalEngine.set(strategy)
            }
            return threadLocalEngine.get()
        }

    fun close() {
        threadLocalEngine.get()?.quit()
        threadLocalEngine.remove()
    }
}

// Global UI syntax
fun open(url: String) = Psenoid.engine.open(url)
fun element(selector: String) = WebElement(By.cssSelector(selector))
fun element(by: By) = WebElement(by)

// Global Network syntax
fun mockRequest(urlPattern: String, response: MockResponse) = Psenoid.engine.mockRequest(urlPattern, response)
fun clearMocks() = Psenoid.engine.clearMocks()
fun waitForRequest(urlSubstring: String, action: () -> Unit) = Psenoid.engine.waitForRequest(urlSubstring, action)
fun waitForResponse(urlSubstring: String, action: () -> Unit) = Psenoid.engine.waitForResponse(urlSubstring, action)

// Global Conditions
val visible = Conditions.visible
fun text(value: String) = Conditions.text(value)
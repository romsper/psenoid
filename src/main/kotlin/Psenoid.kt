import config.EngineType
import config.PsenoidConfig
import elements.CollectionConditions
import elements.Conditions
import elements.WebElement
import elements.WebElements
import engine.BrowserEngine
import engine.PlaywrightStrategy
import engine.SelenideStrategy
import logging.ConsoleScribeListener
import logging.Scribe
import logging.ScribeListener
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

// --- Navigation ---
fun open(url: String) = Psenoid.engine.open(url)
fun back() = Psenoid.engine.back()
fun forward() = Psenoid.engine.forward()
fun refresh() = Psenoid.engine.refresh()
fun getUrl() = Psenoid.engine.getUrl()
fun getTitle() = Psenoid.engine.getTitle()

// --- Element selectors ---
fun element(selector: String) = WebElement(By.cssSelector(selector))
fun element(by: By) = WebElement(by)
fun elements(selector: String) = WebElements(By.cssSelector(selector))
fun elements(by: By) = WebElements(by)
fun count(selector: String) = Psenoid.engine.count(By.cssSelector(selector))
fun count(by: By) = Psenoid.engine.count(by)

// --- Scroll ---
fun scrollBy(x: Int, y: Int) = Psenoid.engine.scrollBy(x, y)

// --- Drag and drop ---
fun dragAndDrop(source: String, target: String) =
    Psenoid.engine.dragAndDrop(By.cssSelector(source), By.cssSelector(target))
fun dragAndDrop(source: By, target: By) = Psenoid.engine.dragAndDrop(source, target)

// --- File upload ---
fun uploadFile(selector: String, filePath: String) = element(selector).uploadFile(filePath)
fun uploadFile(by: By, filePath: String) = element(by).uploadFile(filePath)

// --- Frames ---
fun switchToFrame(selector: String) = Psenoid.engine.switchToFrame(By.cssSelector(selector))
fun switchToFrame(by: By) = Psenoid.engine.switchToFrame(by)
fun switchToDefaultContent() = Psenoid.engine.switchToDefaultContent()

// --- Tabs / Windows ---
fun switchToTab(index: Int) = Psenoid.engine.switchToTab(index)
fun openNewTab() = Psenoid.engine.openNewTab()
fun closeCurrentTab() = Psenoid.engine.closeCurrentTab()
fun getTabCount() = Psenoid.engine.getTabCount()

// --- Dialogs ---
fun acceptAlert() = Psenoid.engine.acceptAlert()
fun dismissAlert() = Psenoid.engine.dismissAlert()
fun getAlertText() = Psenoid.engine.getAlertText()
fun setAlertText(text: String) = Psenoid.engine.setAlertText(text)

// --- JavaScript ---
fun executeScript(script: String, vararg args: Any?) = Psenoid.engine.executeScript(script, *args)

// --- Keyboard ---
fun pressGlobalKey(key: String) = Psenoid.engine.pressGlobalKey(key)

// --- Cookies ---
fun addCookie(name: String, value: String) = Psenoid.engine.addCookie(name, value)
fun getCookie(name: String) = Psenoid.engine.getCookie(name)
fun deleteCookie(name: String) = Psenoid.engine.deleteCookie(name)
fun clearCookies() = Psenoid.engine.clearCookies()
fun getAllCookies() = Psenoid.engine.getAllCookies()

// --- LocalStorage ---
fun setLocalStorage(key: String, value: String) = Psenoid.engine.setLocalStorage(key, value)
fun getLocalStorage(key: String) = Psenoid.engine.getLocalStorage(key)
fun removeLocalStorage(key: String) = Psenoid.engine.removeLocalStorage(key)
fun clearLocalStorage() = Psenoid.engine.clearLocalStorage()

// --- SessionStorage ---
fun setSessionStorage(key: String, value: String) = Psenoid.engine.setSessionStorage(key, value)
fun getSessionStorage(key: String) = Psenoid.engine.getSessionStorage(key)
fun removeSessionStorage(key: String) = Psenoid.engine.removeSessionStorage(key)
fun clearSessionStorage() = Psenoid.engine.clearSessionStorage()

// --- Network interception ---
fun mockRequest(urlPattern: String, response: MockResponse) = Psenoid.engine.mockRequest(urlPattern, response)
fun clearMocks() = Psenoid.engine.clearMocks()
fun waitForRequest(urlSubstring: String, action: () -> Unit) = Psenoid.engine.waitForRequest(urlSubstring, action)
fun waitForResponse(urlSubstring: String, action: () -> Unit) = Psenoid.engine.waitForResponse(urlSubstring, action)

// --- Screenshots ---
fun screenshot() = Psenoid.engine.getScreenshotBytes()

// --- Conditions ---
val visible = Conditions.visible
val invisible = Conditions.invisible
val enabled = Conditions.enabled
val disabled = Conditions.disabled
val checked = Conditions.checked
val unchecked = Conditions.unchecked
val exists = Conditions.exists
fun text(value: String) = Conditions.text(value)
fun textExact(value: String) = Conditions.textExact(value)
fun value(expected: String) = Conditions.value(expected)
fun attribute(name: String, value: String) = Conditions.attribute(name, value)
fun cssValue(property: String, value: String) = Conditions.cssValue(property, value)
fun hasClass(className: String) = Conditions.hasClass(className)

// --- Collection conditions ---
fun size(expected: Int) = CollectionConditions.size(expected)
fun sizeGreaterThan(min: Int) = CollectionConditions.sizeGreaterThan(min)
fun sizeGreaterThanOrEqual(min: Int) = CollectionConditions.sizeGreaterThanOrEqual(min)
fun sizeLessThan(max: Int) = CollectionConditions.sizeLessThan(max)
fun sizeLessThanOrEqual(max: Int) = CollectionConditions.sizeLessThanOrEqual(max)
val empty = CollectionConditions.empty
fun texts(vararg expected: String) = CollectionConditions.texts(*expected)
fun exactTexts(vararg expected: String) = CollectionConditions.exactTexts(*expected)

// --- Logging / reporting ---
fun addListener(listener: ScribeListener) = Scribe.addListener(listener)
fun removeListener(listener: ScribeListener) = Scribe.removeListener(listener)
fun clearListeners() = Scribe.clearListeners()
fun enableConsoleLogging() = Scribe.addListener(ConsoleScribeListener())

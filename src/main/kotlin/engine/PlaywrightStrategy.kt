package engine

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Route
import com.microsoft.playwright.options.Cookie as PlaywrightCookie
import com.microsoft.playwright.options.SelectOption
import config.ConnectionType
import config.PsenoidConfig
import network.MockResponse
import network.NetworkRequest
import network.NetworkResponse
import org.openqa.selenium.By
import java.nio.file.Paths

class PlaywrightStrategy : BrowserEngine {
    private val playwright by lazy { Playwright.create() }
    private val browser by lazy {
        if (PsenoidConfig.connection == ConnectionType.REMOTE) {
            playwright.chromium().connect(PsenoidConfig.wsEndpoint)
        } else {
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(PsenoidConfig.headless))
        }
    }

    private var _currentPage: Page? = null
    private val currentPage: Page
        get() = _currentPage ?: browser.newPage().also { p ->
            _currentPage = p
            setupDialogHandler(p)
        }

    private var frameSelector: String? = null
    private var lastAlertText = ""
    private var acceptDialogs = true
    private var promptText: String? = null

    // Playwright fires every registered dialog handler, and a dialog may only be
    // accepted/dismissed once — so register exactly one handler per page.
    private val dialogHandledPages =
        java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Page, Boolean>())

    private fun setupDialogHandler(page: Page) {
        if (!dialogHandledPages.add(page)) return
        page.onDialog { dialog ->
            lastAlertText = dialog.message()
            val text = promptText
            when {
                !acceptDialogs -> dialog.dismiss()
                dialog.type() == "prompt" && text != null -> dialog.accept(text)
                else -> dialog.accept()
            }
        }
    }

    private fun By.toPwSelector(): String {
        // Index is applied as a native Locator.nth() in locator(); unwrap here.
        if (this is ByNth) return delegate.toPwSelector()
        val str = this.toString()
        return when {
            str.contains("xpath:") -> "xpath=" + str.substringAfter("xpath: ")
            str.contains("cssSelector:") -> str.substringAfter("cssSelector: ")
            str.contains("id:") -> "#" + str.substringAfter("id: ")
            else -> str.substringAfter(": ")
        }
    }

    private fun rawLocator(selector: String): Locator =
        frameSelector?.let { currentPage.frameLocator(it).locator(selector) }
            ?: currentPage.locator(selector)

    private fun locator(by: By): Locator =
        if (by is ByNth) rawLocator(by.delegate.toPwSelector()).nth(by.index)
        else rawLocator(by.toPwSelector())

    // --- Lifecycle ---

    override fun quit() {
        clearMocks()
        try {
            _currentPage?.let { if (!it.isClosed) it.context().browser().close() }
        } finally {
            playwright.close()
        }
    }

    // --- Navigation ---

    override fun open(url: String) { currentPage.navigate(url) }
    override fun back() { currentPage.goBack() }
    override fun forward() { currentPage.goForward() }
    override fun refresh() { currentPage.reload() }
    override fun getUrl(): String = currentPage.url()
    override fun getTitle(): String = currentPage.title()

    // --- Element interaction ---

    override fun click(by: By) { locator(by).click() }
    override fun doubleClick(by: By) { locator(by).dblclick() }
    override fun rightClick(by: By) { locator(by).click(Locator.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT)) }
    override fun hover(by: By) { locator(by).hover() }
    override fun type(by: By, text: String) { locator(by).fill(text) }
    override fun clear(by: By) { locator(by).clear() }
    override fun pressKey(by: By, key: String) { locator(by).press(key) }
    override fun pressGlobalKey(key: String) { currentPage.keyboard().press(key) }
    override fun scrollTo(by: By) { locator(by).scrollIntoViewIfNeeded() }
    override fun scrollBy(x: Int, y: Int) { currentPage.evaluate("window.scrollBy($x, $y)") }

    override fun dragAndDrop(source: By, target: By) {
        locator(source).dragTo(locator(target))
    }

    override fun check(by: By) { locator(by).check() }
    override fun uncheck(by: By) { locator(by).uncheck() }

    override fun selectByValue(by: By, value: String) {
        locator(by).selectOption(value)
    }

    override fun selectByText(by: By, text: String) {
        locator(by).selectOption(SelectOption().setLabel(text))
    }

    override fun selectByIndex(by: By, index: Int) {
        locator(by).selectOption(SelectOption().setIndex(index))
    }

    override fun uploadFile(by: By, filePath: String) {
        locator(by).setInputFiles(Paths.get(filePath))
    }

    override fun focus(by: By) { locator(by).focus() }

    // --- Element state ---

    override fun getText(by: By): String = locator(by).innerText()
    override fun getValue(by: By): String {
        return try { locator(by).inputValue() }
        catch (_: Exception) { locator(by).getAttribute("value") ?: "" }
    }
    override fun getAttribute(by: By, name: String): String? = locator(by).getAttribute(name)
    override fun getCssValue(by: By, property: String): String? =
        locator(by).evaluate("(el, p) => window.getComputedStyle(el).getPropertyValue(p)", property) as? String
    override fun getTagName(by: By): String =
        (locator(by).evaluate("el => el.tagName.toLowerCase()") as? String) ?: ""
    override fun isVisible(by: By): Boolean = locator(by).isVisible
    override fun isEnabled(by: By): Boolean = locator(by).isEnabled
    override fun isChecked(by: By): Boolean = locator(by).isChecked
    override fun exists(by: By): Boolean = locator(by).count() > 0
    override fun count(by: By): Int = locator(by).count()

    // --- Screenshots ---

    override fun getScreenshotBytes(): ByteArray? = currentPage.screenshot()
    override fun getElementScreenshot(by: By): ByteArray? = locator(by).screenshot()

    // --- Frames ---

    override fun switchToFrame(by: By) {
        frameSelector = by.toPwSelector()
    }

    override fun switchToDefaultContent() {
        frameSelector = null
    }

    // --- Tabs / Windows ---

    override fun switchToTab(index: Int) {
        val page = currentPage.context().pages()[index]
        _currentPage = page
        setupDialogHandler(page)
    }

    override fun openNewTab() {
        val page = currentPage.context().newPage()
        _currentPage = page
        setupDialogHandler(page)
    }

    override fun closeCurrentTab() {
        val ctx = currentPage.context()
        currentPage.close()
        val remaining = ctx.pages()
        if (remaining.isNotEmpty()) {
            _currentPage = remaining.last()
            setupDialogHandler(_currentPage!!)
        }
    }

    override fun getTabCount(): Int = currentPage.context().pages().size

    // --- Dialogs ---

    override fun acceptAlert() { acceptDialogs = true }
    override fun dismissAlert() { acceptDialogs = false }
    override fun getAlertText(): String = lastAlertText
    override fun setAlertText(text: String) {
        promptText = text
        acceptDialogs = true
    }

    // --- JavaScript ---

    override fun executeScript(script: String, vararg args: Any?): Any? {
        return if (args.isEmpty()) currentPage.evaluate(script)
        else currentPage.evaluate(script, if (args.size == 1) args[0] else args.toList())
    }

    // --- Cookies ---

    override fun addCookie(name: String, value: String) {
        val cookie = PlaywrightCookie(name, value).apply { url = currentPage.url() }
        currentPage.context().addCookies(listOf(cookie))
    }

    override fun getCookie(name: String): String? =
        currentPage.context().cookies().find { it.name == name }?.value

    override fun deleteCookie(name: String) {
        val remaining = currentPage.context().cookies().filter { it.name != name }
        currentPage.context().clearCookies()
        if (remaining.isNotEmpty()) currentPage.context().addCookies(remaining)
    }

    override fun clearCookies() { currentPage.context().clearCookies() }

    override fun getAllCookies(): Map<String, String> =
        currentPage.context().cookies().associate { it.name to it.value }

    // --- LocalStorage ---

    override fun setLocalStorage(key: String, value: String) {
        currentPage.evaluate("([k,v]) => localStorage.setItem(k, v)", listOf(key, value))
    }

    override fun getLocalStorage(key: String): String? =
        currentPage.evaluate("k => localStorage.getItem(k)", key) as? String

    override fun removeLocalStorage(key: String) {
        currentPage.evaluate("k => localStorage.removeItem(k)", key)
    }

    override fun clearLocalStorage() {
        currentPage.evaluate("() => localStorage.clear()")
    }

    // --- SessionStorage ---

    override fun setSessionStorage(key: String, value: String) {
        currentPage.evaluate("([k,v]) => sessionStorage.setItem(k, v)", listOf(key, value))
    }

    override fun getSessionStorage(key: String): String? =
        currentPage.evaluate("k => sessionStorage.getItem(k)", key) as? String

    override fun removeSessionStorage(key: String) {
        currentPage.evaluate("k => sessionStorage.removeItem(k)", key)
    }

    override fun clearSessionStorage() {
        currentPage.evaluate("() => sessionStorage.clear()")
    }

    // --- Network interception ---

    override fun mockRequest(urlPattern: String, response: MockResponse) {
        currentPage.route(urlPattern) { route ->
            route.fulfill(
                Route.FulfillOptions()
                    .setStatus(response.status)
                    .setContentType(response.contentType)
                    .setBody(response.body)
            )
        }
    }

    override fun clearMocks() { currentPage.unrouteAll() }

    override fun waitForRequest(urlSubstring: String, action: () -> Unit): NetworkRequest {
        val pwReq = currentPage.waitForRequest({ it.url().contains(urlSubstring) }, Runnable { action() })
        return NetworkRequest(pwReq.url(), pwReq.method(), pwReq.headers(), pwReq.postData() ?: "")
    }

    override fun waitForResponse(urlSubstring: String, action: () -> Unit): NetworkResponse {
        val pwResp = currentPage.waitForResponse({ it.url().contains(urlSubstring) }, Runnable { action() })
        return NetworkResponse(pwResp.url(), pwResp.status(), pwResp.headers(), pwResp.text() ?: "")
    }
}

package engine

import com.codeborne.selenide.Condition as SelenideCondition
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner
import config.ConnectionType
import config.PsenoidConfig
import io.netty.handler.codec.http.HttpResponseStatus
import network.MockResponse
import network.NetworkRequest
import network.NetworkResponse
import org.openqa.selenium.By
import org.openqa.selenium.Cookie as SeleniumCookie
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.support.ui.Select
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SelenideStrategy : BrowserEngine {
    private val activeMocks = ConcurrentHashMap<String, MockResponse>()
    private var mockFilterRegistered = false
    private var isNetworkListenerInitialized = false
    private val requestListeners = CopyOnWriteArrayList<(NetworkRequest) -> Unit>()
    private val responseListeners = CopyOnWriteArrayList<(NetworkResponse) -> Unit>()

    init {
        Configuration.browser = PsenoidConfig.browser
        Configuration.headless = PsenoidConfig.headless
        Configuration.proxyEnabled = true

        if (PsenoidConfig.connection == ConnectionType.REMOTE) {
            Configuration.remote = PsenoidConfig.remoteUrl
        }
    }

    private fun stringToKeys(key: String): CharSequence =
        try { Keys.valueOf(key.uppercase()) } catch (e: IllegalArgumentException) { key }

    private fun driver() = WebDriverRunner.getWebDriver()

    // --- Lifecycle ---

    override fun quit() {
        clearMocks()
        Selenide.closeWebDriver()
    }

    // --- Navigation ---

    override fun open(url: String) = Selenide.open(url)
    override fun back() = Selenide.back()
    override fun forward() = Selenide.forward()
    override fun refresh() = Selenide.refresh()
    override fun getUrl(): String = driver().currentUrl ?: ""
    override fun getTitle(): String = driver().title ?: ""

    // --- Element interaction ---

    override fun click(by: By) { Selenide.`$`(by).click() }
    override fun doubleClick(by: By) { Selenide.`$`(by).doubleClick() }
    override fun rightClick(by: By) { Selenide.`$`(by).contextClick() }
    override fun hover(by: By) { Selenide.`$`(by).hover() }
    override fun type(by: By, text: String) { Selenide.`$`(by).setValue(text) }
    override fun clear(by: By) { Selenide.`$`(by).clear() }
    override fun pressKey(by: By, key: String) { Selenide.`$`(by).sendKeys(stringToKeys(key)) }
    override fun pressGlobalKey(key: String) { Selenide.actions().sendKeys(stringToKeys(key)).perform() }
    override fun scrollTo(by: By) { Selenide.`$`(by).scrollTo() }
    override fun scrollBy(x: Int, y: Int) { Selenide.executeJavaScript<Unit>("window.scrollBy($x, $y)") }

    override fun dragAndDrop(source: By, target: By) {
        Selenide.actions()
            .dragAndDrop(Selenide.`$`(source).toWebElement(), Selenide.`$`(target).toWebElement())
            .perform()
    }

    override fun check(by: By) {
        val el = Selenide.`$`(by)
        if (!el.isSelected) el.click()
    }

    override fun uncheck(by: By) {
        val el = Selenide.`$`(by)
        if (el.isSelected) el.click()
    }

    override fun selectByValue(by: By, value: String) {
        Select(Selenide.`$`(by).toWebElement()).selectByValue(value)
    }

    override fun selectByText(by: By, text: String) {
        Select(Selenide.`$`(by).toWebElement()).selectByVisibleText(text)
    }

    override fun selectByIndex(by: By, index: Int) {
        Select(Selenide.`$`(by).toWebElement()).selectByIndex(index)
    }

    override fun uploadFile(by: By, filePath: String) {
        Selenide.`$`(by).uploadFile(File(filePath))
    }

    override fun focus(by: By) {
        Selenide.executeJavaScript<Unit>("arguments[0].focus()", Selenide.`$`(by).toWebElement())
    }

    // --- Element state ---

    override fun getText(by: By): String = Selenide.`$`(by).text()
    override fun getValue(by: By): String = Selenide.`$`(by).getValue() ?: ""
    override fun getAttribute(by: By, name: String): String? = Selenide.`$`(by).getAttribute(name)
    override fun getCssValue(by: By, property: String): String? = Selenide.`$`(by).getCssValue(property)
    override fun getTagName(by: By): String = Selenide.`$`(by).tagName
    override fun isVisible(by: By): Boolean = Selenide.`$`(by).`is`(SelenideCondition.visible)
    override fun isEnabled(by: By): Boolean = Selenide.`$`(by).isEnabled
    override fun isChecked(by: By): Boolean = Selenide.`$`(by).isSelected
    override fun exists(by: By): Boolean = Selenide.`$`(by).exists()
    override fun count(by: By): Int = Selenide.`$$`(by).size()

    // --- Screenshots ---

    override fun getScreenshotBytes(): ByteArray? {
        return try {
            (driver() as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
        } catch (_: WebDriverException) { null }
    }

    override fun getElementScreenshot(by: By): ByteArray? {
        return try {
            (Selenide.`$`(by).toWebElement() as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
        } catch (_: Exception) { null }
    }

    // --- Frames ---

    override fun switchToFrame(by: By) {
        driver().switchTo().frame(Selenide.`$`(by).toWebElement())
    }

    override fun switchToDefaultContent() {
        driver().switchTo().defaultContent()
    }

    // --- Tabs / Windows ---

    override fun switchToTab(index: Int) {
        val handles = driver().windowHandles.toList()
        driver().switchTo().window(handles[index])
    }

    override fun openNewTab() {
        Selenide.executeJavaScript<Unit>("window.open('')")
        val handles = driver().windowHandles.toList()
        driver().switchTo().window(handles.last())
    }

    override fun closeCurrentTab() {
        val d = driver()
        d.close()
        try {
            val remaining = d.windowHandles.toList()
            if (remaining.isNotEmpty()) d.switchTo().window(remaining.last())
        } catch (_: Exception) { /* last window closed, session is done */ }
    }

    override fun getTabCount(): Int = driver().windowHandles.size

    // --- Dialogs ---

    override fun acceptAlert() { driver().switchTo().alert().accept() }
    override fun dismissAlert() { driver().switchTo().alert().dismiss() }
    override fun getAlertText(): String = driver().switchTo().alert().text
    override fun setAlertText(text: String) {
        val alert = driver().switchTo().alert()
        alert.sendKeys(text)
        alert.accept()
    }

    // --- JavaScript ---

    @Suppress("UNCHECKED_CAST")
    override fun executeScript(script: String, vararg args: Any?): Any? =
        Selenide.executeJavaScript(script, *(args as Array<Any>))

    // --- Cookies ---

    override fun addCookie(name: String, value: String) {
        driver().manage().addCookie(SeleniumCookie(name, value))
    }

    override fun getCookie(name: String): String? =
        driver().manage().getCookieNamed(name)?.value

    override fun deleteCookie(name: String) {
        driver().manage().deleteCookieNamed(name)
    }

    override fun clearCookies() {
        driver().manage().deleteAllCookies()
    }

    override fun getAllCookies(): Map<String, String> =
        driver().manage().cookies.associate { it.name to it.value }

    // --- LocalStorage ---

    override fun setLocalStorage(key: String, value: String) {
        Selenide.executeJavaScript<Unit>("localStorage.setItem(arguments[0], arguments[1])", key, value)
    }

    override fun getLocalStorage(key: String): String? =
        Selenide.executeJavaScript("return localStorage.getItem(arguments[0])", key)

    override fun removeLocalStorage(key: String) {
        Selenide.executeJavaScript<Unit>("localStorage.removeItem(arguments[0])", key)
    }

    override fun clearLocalStorage() {
        Selenide.executeJavaScript<Unit>("localStorage.clear()")
    }

    // --- SessionStorage ---

    override fun setSessionStorage(key: String, value: String) {
        Selenide.executeJavaScript<Unit>("sessionStorage.setItem(arguments[0], arguments[1])", key, value)
    }

    override fun getSessionStorage(key: String): String? =
        Selenide.executeJavaScript("return sessionStorage.getItem(arguments[0])", key)

    override fun removeSessionStorage(key: String) {
        Selenide.executeJavaScript<Unit>("sessionStorage.removeItem(arguments[0])", key)
    }

    override fun clearSessionStorage() {
        Selenide.executeJavaScript<Unit>("sessionStorage.clear()")
    }

    // --- Network interception ---

    private fun ensureMockFilterRegistered() {
        if (mockFilterRegistered) return
        val proxy = WebDriverRunner.getSelenideProxy().proxy
        proxy.addResponseFilter { responseObj, contents, messageInfo ->
            val mock = activeMocks.entries.find { (pattern, _) ->
                messageInfo.originalUrl.contains(pattern.replace("*", ""))
            }?.value ?: return@addResponseFilter
            contents.textContents = mock.body
            responseObj.status = HttpResponseStatus.valueOf(mock.status)
            responseObj.headers().set("Content-Type", mock.contentType)
        }
        mockFilterRegistered = true
    }

    override fun mockRequest(urlPattern: String, response: MockResponse) {
        ensureMockFilterRegistered()
        activeMocks[urlPattern] = response
    }

    override fun clearMocks() { activeMocks.clear() }

    private fun initNetworkListenersIfNeeded() {
        if (isNetworkListenerInitialized) return
        val proxy = WebDriverRunner.getSelenideProxy().proxy

        proxy.addRequestFilter { request, contents, messageInfo ->
            val req = NetworkRequest(
                url = messageInfo.originalUrl,
                method = request.method().name(),
                headers = request.headers().associate { it.key to it.value },
                body = contents.textContents ?: ""
            )
            requestListeners.forEach { it.invoke(req) }
            null
        }

        proxy.addResponseFilter { response, contents, messageInfo ->
            val resp = NetworkResponse(
                url = messageInfo.originalUrl,
                status = response.status().code(),
                headers = response.headers().associate { it.key to it.value },
                body = contents.textContents ?: ""
            )
            responseListeners.forEach { it.invoke(resp) }
        }
        isNetworkListenerInitialized = true
    }

    override fun waitForRequest(urlSubstring: String, action: () -> Unit): NetworkRequest {
        initNetworkListenersIfNeeded()
        var captured: NetworkRequest? = null
        val listener: (NetworkRequest) -> Unit = { if (it.url.contains(urlSubstring) && captured == null) captured = it }

        requestListeners.add(listener)
        try {
            action()
            val end = System.currentTimeMillis() + PsenoidConfig.timeout.toMillis()
            while (System.currentTimeMillis() < end) {
                captured?.let { return it }
                Thread.sleep(50)
            }
            throw RuntimeException("Timeout waiting for request containing: $urlSubstring")
        } finally {
            requestListeners.remove(listener)
        }
    }

    override fun waitForResponse(urlSubstring: String, action: () -> Unit): NetworkResponse {
        initNetworkListenersIfNeeded()
        var captured: NetworkResponse? = null
        val listener: (NetworkResponse) -> Unit = { if (it.url.contains(urlSubstring) && captured == null) captured = it }

        responseListeners.add(listener)
        try {
            action()
            val end = System.currentTimeMillis() + PsenoidConfig.timeout.toMillis()
            while (System.currentTimeMillis() < end) {
                if (captured != null) return captured
                Thread.sleep(50)
            }
            throw RuntimeException("Timeout waiting for response containing: $urlSubstring")
        } finally {
            responseListeners.remove(listener)
        }
    }
}

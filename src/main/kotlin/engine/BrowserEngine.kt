package engine

import network.MockResponse
import network.NetworkRequest
import network.NetworkResponse
import org.openqa.selenium.By

interface BrowserEngine {

    // Lifecycle
    fun quit()

    // Navigation
    fun open(url: String)
    fun back()
    fun forward()
    fun refresh()
    fun getUrl(): String
    fun getTitle(): String

    // Element interaction
    fun click(by: By)
    fun doubleClick(by: By)
    fun rightClick(by: By)
    fun hover(by: By)
    fun type(by: By, text: String)
    fun clear(by: By)
    fun pressKey(by: By, key: String)
    fun pressGlobalKey(key: String)
    fun scrollTo(by: By)
    fun scrollBy(x: Int, y: Int)
    fun dragAndDrop(source: By, target: By)
    fun check(by: By)
    fun uncheck(by: By)
    fun selectByValue(by: By, value: String)
    fun selectByText(by: By, text: String)
    fun selectByIndex(by: By, index: Int)
    fun uploadFile(by: By, filePath: String)
    fun focus(by: By)

    // Element state
    fun getText(by: By): String
    fun getValue(by: By): String
    fun getAttribute(by: By, name: String): String?
    fun getCssValue(by: By, property: String): String?
    fun getTagName(by: By): String
    fun isVisible(by: By): Boolean
    fun isEnabled(by: By): Boolean
    fun isChecked(by: By): Boolean
    fun exists(by: By): Boolean
    fun count(by: By): Int

    // Screenshots
    fun getScreenshotBytes(): ByteArray?
    fun getElementScreenshot(by: By): ByteArray?

    // Frames
    fun switchToFrame(by: By)
    fun switchToDefaultContent()

    // Tabs / Windows
    fun switchToTab(index: Int)
    fun openNewTab()
    fun closeCurrentTab()
    fun getTabCount(): Int

    // Dialogs
    fun acceptAlert()
    fun dismissAlert()
    fun getAlertText(): String
    fun setAlertText(text: String)

    // JavaScript
    fun executeScript(script: String, vararg args: Any?): Any?

    // Cookies
    fun addCookie(name: String, value: String)
    fun getCookie(name: String): String?
    fun deleteCookie(name: String)
    fun clearCookies()
    fun getAllCookies(): Map<String, String>

    // LocalStorage
    fun setLocalStorage(key: String, value: String)
    fun getLocalStorage(key: String): String?
    fun removeLocalStorage(key: String)
    fun clearLocalStorage()

    // SessionStorage
    fun setSessionStorage(key: String, value: String)
    fun getSessionStorage(key: String): String?
    fun removeSessionStorage(key: String)
    fun clearSessionStorage()

    // Network interception
    fun mockRequest(urlPattern: String, response: MockResponse)
    fun clearMocks()
    fun waitForRequest(urlSubstring: String, action: () -> Unit): NetworkRequest
    fun waitForResponse(urlSubstring: String, action: () -> Unit): NetworkResponse
}

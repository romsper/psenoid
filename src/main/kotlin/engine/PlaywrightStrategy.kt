package engine

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Route
import config.ConnectionType
import config.PsenoidConfig
import network.MockResponse
import network.NetworkRequest
import network.NetworkResponse
import org.openqa.selenium.By

class PlaywrightStrategy : BrowserEngine {
    private val playwright by lazy { Playwright.create() }
    private val page by lazy {
        val browser = if (PsenoidConfig.connection == ConnectionType.REMOTE) {
            playwright.chromium().connect(PsenoidConfig.wsEndpoint)
        } else {
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(PsenoidConfig.headless))
        }
        browser.newPage()
    }

    private fun By.toPwSelector(): String {
        val str = this.toString()
        return when {
            str.contains("xpath:") -> "xpath=" + str.substringAfter("xpath: ")
            str.contains("cssSelector:") -> str.substringAfter("cssSelector: ")
            str.contains("id:") -> "#" + str.substringAfter("id: ")
            else -> str.substringAfter(": ")
        }
    }

    override fun open(url: String) { page.navigate(url) }
    override fun click(by: By) { page.locator(by.toPwSelector()).click() }
    override fun type(by: By, text: String) { page.locator(by.toPwSelector()).fill(text) }
    override fun getText(by: By): String = page.locator(by.toPwSelector()).innerText()
    override fun isVisible(by: By): Boolean = page.locator(by.toPwSelector()).isVisible
    override fun quit() {
        if (page.context() != null) page.context().browser().close()
        playwright.close()
    }
    override fun getScreenshotBytes(): ByteArray? = page.screenshot()

    override fun mockRequest(urlPattern: String, response: MockResponse) {
        page.route(urlPattern) { route ->
            route.fulfill(
                Route.FulfillOptions()
                    .setStatus(response.status)
                    .setContentType(response.contentType)
                    .setBody(response.body)
            )
        }
    }

    override fun clearMocks() { page.unrouteAll() }

    override fun waitForRequest(urlSubstring: String, action: () -> Unit): NetworkRequest {
        val pwReq = page.waitForRequest({ it.url().contains(urlSubstring) }, Runnable { action() })
        return NetworkRequest(pwReq.url(), pwReq.method(), pwReq.headers(), pwReq.postData() ?: "")
    }

    override fun waitForResponse(urlSubstring: String, action: () -> Unit): NetworkResponse {
        val pwResp = page.waitForResponse({ it.url().contains(urlSubstring) }, Runnable { action() })
        return NetworkResponse(pwResp.url(), pwResp.status(), pwResp.headers(), pwResp.text() ?: "")
    }
}
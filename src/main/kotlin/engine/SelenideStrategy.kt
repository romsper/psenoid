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
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriverException
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
        Configuration.proxyEnabled = true // Enable proxy for network interception

        if (PsenoidConfig.connection == ConnectionType.REMOTE) {
            Configuration.remote = PsenoidConfig.remoteUrl
        }
    }

    override fun open(url: String) = Selenide.open(url)
    override fun click(by: By) { Selenide.`$`(by).click() }
    override fun type(by: By, text: String) { Selenide.`$`(by).setValue(text) }
    override fun getText(by: By): String = Selenide.`$`(by).text()
    override fun isVisible(by: By): Boolean = Selenide.`$`(by).`is`(SelenideCondition.visible)

    override fun quit() {
        clearMocks()
        Selenide.closeWebDriver()
    }

    override fun getScreenshotBytes(): ByteArray? {
        return try {
            (Selenide.webdriver().`object`() as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
        } catch (e: WebDriverException) { null }
    }

    private fun ensureMockFilterRegistered() {
        if (mockFilterRegistered) return
        val proxy = WebDriverRunner.getSelenideProxy()?.proxy ?: return
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

    override fun clearMocks() {
        activeMocks.clear()
    }

    private fun initNetworkListenersIfNeeded() {
        if (isNetworkListenerInitialized) return
        val proxy = WebDriverRunner.getSelenideProxy()?.proxy ?: return

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
                if (captured != null) return captured!!
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
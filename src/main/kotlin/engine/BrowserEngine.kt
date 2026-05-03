package engine

import network.MockResponse
import network.NetworkRequest
import network.NetworkResponse
import org.openqa.selenium.By

interface BrowserEngine {
    fun open(url: String)
    fun click(by: By)
    fun type(by: By, text: String)
    fun getText(by: By): String
    fun isVisible(by: By): Boolean
    fun quit()
    fun getScreenshotBytes(): ByteArray?

    // Network Interception
    fun mockRequest(urlPattern: String, response: MockResponse)
    fun clearMocks()
    fun waitForRequest(urlSubstring: String, action: () -> Unit): NetworkRequest
    fun waitForResponse(urlSubstring: String, action: () -> Unit): NetworkResponse
}
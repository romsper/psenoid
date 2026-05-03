package elements

import Psenoid
import config.PsenoidConfig
import logging.Scribe
import org.openqa.selenium.By
import java.time.Duration

class WebElement(
    val selector: By,
    private val customTimeout: Duration? = null
) {
    val timeout: Duration get() = customTimeout ?: PsenoidConfig.timeout

    fun withTimeout(duration: Duration) = WebElement(selector, duration)

    private fun <T> waitFor(actionName: String, action: (By) -> T): T {
        return Scribe.step(actionName) {
            val end = System.currentTimeMillis() + timeout.toMillis()
            var lastError: Throwable? = null

            while (System.currentTimeMillis() < end) {
                try {
                    return@step action(selector)
                } catch (e: Throwable) {
                    lastError = e
                    Thread.sleep(100)
                }
            }
            throw RuntimeException("Timeout during: $actionName", lastError)
        }
    }

    fun click() = waitFor("Click on [$selector]") { Psenoid.engine.click(it) }
    fun type(text: String) = waitFor("Type '$text' into [$selector]") { Psenoid.engine.type(it, text) }
    fun getText(): String = waitFor("Get text from [$selector]") { Psenoid.engine.getText(it) }

    fun wait(condition: Condition): WebElement {
        waitFor("Wait for ${condition.javaClass.simpleName} on [$selector]") {
            if (!condition.check(this)) throw AssertionError(condition.errorMessage(this))
        }
        return this
    }
}
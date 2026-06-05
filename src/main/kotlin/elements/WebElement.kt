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

    // --- Basic interaction ---

    fun click() = apply { waitFor("Click on [$selector]") { Psenoid.engine.click(it) } }
    fun doubleClick() = apply { waitFor("Double click on [$selector]") { Psenoid.engine.doubleClick(it) } }
    fun rightClick() = apply { waitFor("Right click on [$selector]") { Psenoid.engine.rightClick(it) } }
    fun hover() = apply { waitFor("Hover over [$selector]") { Psenoid.engine.hover(it) } }
    fun type(text: String) = apply { waitFor("Type '$text' into [$selector]") { Psenoid.engine.type(it, text) } }
    fun clear() = apply { waitFor("Clear [$selector]") { Psenoid.engine.clear(it) } }
    fun pressKey(key: String) = apply { waitFor("Press '$key' on [$selector]") { Psenoid.engine.pressKey(it, key) } }
    fun scrollTo() = apply { waitFor("Scroll to [$selector]") { Psenoid.engine.scrollTo(it) } }
    fun focus() = apply { waitFor("Focus [$selector]") { Psenoid.engine.focus(it) } }

    // --- Form controls ---

    fun check() = apply { waitFor("Check [$selector]") { Psenoid.engine.check(it) } }
    fun uncheck() = apply { waitFor("Uncheck [$selector]") { Psenoid.engine.uncheck(it) } }
    fun selectByValue(value: String) = apply { waitFor("Select by value '$value' in [$selector]") { Psenoid.engine.selectByValue(it, value) } }
    fun selectByText(text: String) = apply { waitFor("Select by text '$text' in [$selector]") { Psenoid.engine.selectByText(it, text) } }
    fun selectByIndex(index: Int) = apply { waitFor("Select by index $index in [$selector]") { Psenoid.engine.selectByIndex(it, index) } }
    fun uploadFile(filePath: String) = apply { waitFor("Upload file '$filePath' to [$selector]") { Psenoid.engine.uploadFile(it, filePath) } }

    // --- Drag and drop ---

    fun dragTo(target: WebElement) = apply {
        waitFor("Drag [$selector] to [${target.selector}]") { Psenoid.engine.dragAndDrop(it, target.selector) }
    }

    fun dragTo(target: By) = apply {
        waitFor("Drag [$selector] to [$target]") { Psenoid.engine.dragAndDrop(it, target) }
    }

    // --- Read state ---

    fun getText(): String = waitFor("Get text from [$selector]") { Psenoid.engine.getText(it) }
    fun getValue(): String = waitFor("Get value from [$selector]") { Psenoid.engine.getValue(it) }
    fun getAttribute(name: String): String? = waitFor("Get attribute '$name' from [$selector]") { Psenoid.engine.getAttribute(it, name) }
    fun getCssValue(property: String): String? = waitFor("Get CSS '$property' from [$selector]") { Psenoid.engine.getCssValue(it, property) }
    fun getTagName(): String = waitFor("Get tag name of [$selector]") { Psenoid.engine.getTagName(it) }
    fun isEnabled(): Boolean = waitFor("Check enabled state of [$selector]") { Psenoid.engine.isEnabled(it) }
    fun isChecked(): Boolean = waitFor("Check checked state of [$selector]") { Psenoid.engine.isChecked(it) }
    fun exists(): Boolean = try { Psenoid.engine.exists(selector) } catch (_: Exception) { false }
    fun count(): Int = Psenoid.engine.count(selector)
    fun screenshot(): ByteArray? = Psenoid.engine.getElementScreenshot(selector)

    // --- Conditions ---

    fun wait(condition: Condition): WebElement {
        waitFor("Wait for ${condition.javaClass.simpleName} on [$selector]") {
            if (!condition.check(this)) throw AssertionError(condition.errorMessage(this))
        }
        return this
    }
}

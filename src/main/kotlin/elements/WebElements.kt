package elements

import Psenoid
import config.PsenoidConfig
import engine.ByNth
import logging.Scribe
import org.openqa.selenium.By
import java.time.Duration

/**
 * A lazy, live collection of elements matching [selector] — the Psenoid
 * equivalent of Selenide's `$$` / Playwright's multi-match locator.
 *
 * The size is queried from the engine on demand, and each item is a [WebElement]
 * scoped to its position via [ByNth], so it re-resolves on every action and
 * carries the full single-element API (click, type, conditions, …).
 */
class WebElements(
    val selector: By,
    private val customTimeout: Duration? = null
) : Iterable<WebElement> {

    val timeout: Duration get() = customTimeout ?: PsenoidConfig.timeout

    fun size(): Int = Psenoid.engine.count(selector)
    fun isEmpty(): Boolean = size() == 0
    fun isNotEmpty(): Boolean = size() > 0

    /** The element at [index]; supports both `collection[i]` and `collection.get(i)`. */
    operator fun get(index: Int): WebElement {
        val element = WebElement(ByNth(selector, index))
        return customTimeout?.let { element.withTimeout(it) } ?: element
    }

    fun first(): WebElement = get(0)
    fun last(): WebElement = get(size() - 1)

    /** Snapshot of the current matches as individual elements. */
    fun toList(): List<WebElement> = (0 until size()).map { get(it) }

    /** Visible text of every matched element, in document order. */
    fun texts(): List<String> = (0 until size()).map { Psenoid.engine.getText(ByNth(selector, it)) }

    fun withTimeout(duration: Duration) = WebElements(selector, duration)

    override fun iterator(): Iterator<WebElement> = toList().iterator()

    // --- Conditions ---

    /** Polls [condition] every 100ms until it holds or [timeout] elapses, then fails. */
    fun wait(condition: CollectionCondition): WebElements {
        Scribe.step("Wait for ${condition.javaClass.simpleName} on [$selector]") {
            val end = System.currentTimeMillis() + timeout.toMillis()
            var lastError: Throwable? = null
            while (System.currentTimeMillis() < end) {
                try {
                    if (condition.check(this)) return@step
                } catch (e: Throwable) {
                    lastError = e
                }
                Thread.sleep(100)
            }
            throw AssertionError(condition.errorMessage(this), lastError)
        }
        return this
    }

    fun shouldHaveSize(expected: Int): WebElements = wait(CollectionConditions.size(expected))
    fun shouldBeEmpty(): WebElements = wait(CollectionConditions.empty)
}

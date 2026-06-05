package engine

import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement

/**
 * A [By] that resolves to a single element — the one at [index] among all
 * elements matching [delegate].
 *
 * This lets an indexed element taken from a collection flow through the existing
 * By-based [BrowserEngine] API unchanged: Selenide resolves it via the standard
 * WebDriver lookup (this class' [findElements]), while [PlaywrightStrategy] maps
 * it to a native `Locator.nth(index)`.
 */
class ByNth(val delegate: By, val index: Int) : By() {

    override fun findElements(context: SearchContext): MutableList<WebElement> {
        val all = context.findElements(delegate)
        return if (index in all.indices) mutableListOf(all[index]) else mutableListOf()
    }

    override fun toString(): String = "$delegate[$index]"
}

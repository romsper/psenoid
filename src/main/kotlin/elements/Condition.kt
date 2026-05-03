package elements

import Psenoid // Added import for Psenoid

abstract class Condition {
    abstract fun check(element: elements.WebElement): Boolean
    abstract fun errorMessage(element: elements.WebElement): String
}

object Conditions {
    val visible = object : Condition() {
        override fun check(element: elements.WebElement) = Psenoid.engine.isVisible(element.selector)
        override fun errorMessage(element: elements.WebElement) = "Element [${element.selector}] should be visible"
    }

    fun text(expected: String) = object : Condition() {
        override fun check(element: elements.WebElement): Boolean {
            return try { Psenoid.engine.getText(element.selector).contains(expected) } catch (e: Exception) { false }
        }
        override fun errorMessage(element: elements.WebElement) = "Element [${element.selector}] should contain text '$expected'"
    }
}
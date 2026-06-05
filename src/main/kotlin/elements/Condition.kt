package elements

import Psenoid

abstract class Condition {
    abstract fun check(element: WebElement): Boolean
    abstract fun errorMessage(element: WebElement): String
}

object Conditions {

    val visible = object : Condition() {
        override fun check(element: WebElement) = Psenoid.engine.isVisible(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should be visible"
    }

    val invisible = object : Condition() {
        override fun check(element: WebElement) = !Psenoid.engine.isVisible(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should not be visible"
    }

    val enabled = object : Condition() {
        override fun check(element: WebElement) = Psenoid.engine.isEnabled(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should be enabled"
    }

    val disabled = object : Condition() {
        override fun check(element: WebElement) = !Psenoid.engine.isEnabled(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should be disabled"
    }

    val checked = object : Condition() {
        override fun check(element: WebElement) = Psenoid.engine.isChecked(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should be checked"
    }

    val unchecked = object : Condition() {
        override fun check(element: WebElement) = !Psenoid.engine.isChecked(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should be unchecked"
    }

    val exists = object : Condition() {
        override fun check(element: WebElement) = Psenoid.engine.exists(element.selector)
        override fun errorMessage(element: WebElement) = "Element [${element.selector}] should exist in DOM"
    }

    fun text(expected: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try { Psenoid.engine.getText(element.selector).contains(expected) } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should contain text '$expected'"
    }

    fun textExact(expected: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try { Psenoid.engine.getText(element.selector) == expected } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should have exact text '$expected'"
    }

    fun value(expected: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try { Psenoid.engine.getValue(element.selector) == expected } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should have value '$expected'"
    }

    fun attribute(name: String, expected: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try { Psenoid.engine.getAttribute(element.selector, name) == expected } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should have attribute '$name' = '$expected'"
    }

    fun cssValue(property: String, expected: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try { Psenoid.engine.getCssValue(element.selector, property) == expected } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should have CSS '$property' = '$expected'"
    }

    fun hasClass(className: String) = object : Condition() {
        override fun check(element: WebElement): Boolean =
            try {
                Psenoid.engine.getAttribute(element.selector, "class")
                    ?.split(" ")?.contains(className) == true
            } catch (_: Exception) { false }
        override fun errorMessage(element: WebElement) =
            "Element [${element.selector}] should have class '$className'"
    }
}

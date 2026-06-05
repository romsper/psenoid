package elements

/** A condition evaluated against a whole [WebElements] collection (size, texts, …). */
abstract class CollectionCondition {
    abstract fun check(elements: WebElements): Boolean
    abstract fun errorMessage(elements: WebElements): String
}

object CollectionConditions {

    fun size(expected: Int) = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() == expected
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have size $expected but was ${elements.size()}"
    }

    fun sizeGreaterThan(min: Int) = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() > min
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have size > $min but was ${elements.size()}"
    }

    fun sizeGreaterThanOrEqual(min: Int) = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() >= min
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have size >= $min but was ${elements.size()}"
    }

    fun sizeLessThan(max: Int) = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() < max
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have size < $max but was ${elements.size()}"
    }

    fun sizeLessThanOrEqual(max: Int) = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() <= max
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have size <= $max but was ${elements.size()}"
    }

    val empty = object : CollectionCondition() {
        override fun check(elements: WebElements) = elements.size() == 0
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should be empty but had ${elements.size()} element(s)"
    }

    /** Each element, in order, contains the corresponding expected substring. */
    fun texts(vararg expected: String) = object : CollectionCondition() {
        override fun check(elements: WebElements): Boolean {
            val actual = elements.texts()
            return actual.size == expected.size &&
                actual.zip(expected.toList()).all { (a, e) -> a.contains(e) }
        }
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] texts should contain ${expected.toList()} but was ${elements.texts()}"
    }

    /** Element texts equal the expected list exactly, in order. */
    fun exactTexts(vararg expected: String) = object : CollectionCondition() {
        override fun check(elements: WebElements): Boolean = elements.texts() == expected.toList()
        override fun errorMessage(elements: WebElements) =
            "Collection [${elements.selector}] should have exact texts ${expected.toList()} but was ${elements.texts()}"
    }
}

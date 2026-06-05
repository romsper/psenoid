import config.EngineType
import config.PsenoidConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.time.Duration

/**
 * Verifies that the unified API behaves identically on both engines.
 * Each test runs once per [EngineType] against a local, hermetic HTML fixture
 * (no network), headless.
 */
class ParityTest {

    private val fixtureUrl: String get() = FixtureServer.url

    @AfterEach
    fun tearDown() = Psenoid.close()

    private fun start(engine: EngineType) {
        PsenoidConfig.engine = engine
        PsenoidConfig.headless = true
        PsenoidConfig.timeout = Duration.ofSeconds(10)
        open(fixtureUrl)
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `title and text`(engine: EngineType) {
        start(engine)
        assertEquals("Psenoid Fixture Page", getTitle())
        element("#title").wait(visible)
        element("#title").wait(text("Fixture"))
        assertEquals("Psenoid Fixture", element("#title").getText())
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `click updates state and reveals element`(engine: EngineType) {
        start(engine)
        element("#box").wait(invisible)
        element("#status").wait(textExact("idle"))
        element("#btn").click()
        element("#status").wait(textExact("clicked"))
        element("#box").wait(visible)
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `type and read value`(engine: EngineType) {
        start(engine)
        element("#name").type("psenoid")
        assertEquals("psenoid", element("#name").getValue())
        element("#name").wait(value("psenoid"))
        element("#name").clear()
        element("#name").wait(value(""))
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `checkbox check and uncheck`(engine: EngineType) {
        start(engine)
        element("#agree").wait(unchecked)
        element("#agree").check()
        element("#agree").wait(checked)
        assertTrue(element("#agree").isChecked())
        element("#agree").uncheck()
        element("#agree").wait(unchecked)
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `select option by value text and index`(engine: EngineType) {
        start(engine)
        element("#fruit").selectByValue("b")
        assertEquals("b", element("#fruit").getValue())
        element("#fruit").selectByText("Cherry")
        assertEquals("c", element("#fruit").getValue())
        element("#fruit").selectByIndex(0)
        assertEquals("a", element("#fruit").getValue())
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `attributes classes and count`(engine: EngineType) {
        start(engine)
        assertEquals("hero", element("#tagged").getAttribute("data-role"))
        element("#tagged").wait(attribute("data-role", "hero"))
        element("#tagged").wait(hasClass("alpha"))
        element("#tagged").wait(hasClass("beta"))
        assertEquals("div", element("#tagged").getTagName().lowercase())
        assertEquals(3, count(".item"))
        assertEquals(3, element(".item").count())
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `visibility and existence`(engine: EngineType) {
        start(engine)
        element("#hidden").wait(invisible)
        assertTrue(element("#hidden").exists())
        assertTrue(element("#title").exists())
        assertFalse(element("#does-not-exist").exists())
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `local and session storage`(engine: EngineType) {
        start(engine)
        setLocalStorage("k", "v")
        assertEquals("v", getLocalStorage("k"))
        removeLocalStorage("k")
        assertNull(getLocalStorage("k"))

        setSessionStorage("sk", "sv")
        assertEquals("sv", getSessionStorage("sk"))
        clearSessionStorage()
        assertNull(getSessionStorage("sk"))
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `iframe content`(engine: EngineType) {
        start(engine)
        switchToFrame("#frame")
        assertEquals("inside", element("#inframe").getText())
        switchToDefaultContent()
        element("#title").wait(visible)
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `scroll does not throw`(engine: EngineType) {
        start(engine)
        scrollBy(0, 50)
        element("#tagged").scrollTo()
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `file upload`(engine: EngineType) {
        start(engine)
        val path = File("src/test/resources/upload.txt").absolutePath

        uploadFile("#file", path)

        assertTrue(element("#file").getValue().contains("upload.txt"))
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `elements collection`(engine: EngineType) {
        start(engine)
        val items = elements(".item")

        assertEquals(3, items.size())
        assertTrue(items.isNotEmpty())
        assertEquals(listOf("one", "two", "three"), items.texts())
        assertEquals("two", items[1].getText())
        assertEquals("one", items.first().getText())
        assertEquals("three", items.last().getText())
        assertEquals(3, items.toList().size)
        assertEquals(3, items.count()) // via Iterable
        assertTrue(elements(".nope").isEmpty())

        // an indexed item carries the full, interactive element API
        elements("button")[0].click()
        element("#status").wait(textExact("clicked"))
    }

    @ParameterizedTest
    @EnumSource(EngineType::class)
    fun `collection wait conditions`(engine: EngineType) {
        start(engine)
        val items = elements(".item")

        items.wait(size(3))
        items.shouldHaveSize(3)
        items.wait(sizeGreaterThan(2))
        items.wait(sizeGreaterThanOrEqual(3))
        items.wait(sizeLessThan(4))
        items.wait(sizeLessThanOrEqual(3))
        items.wait(exactTexts("one", "two", "three"))
        items.wait(texts("on", "tw", "th"))
        elements(".nope").wait(empty)
        elements(".nope").shouldBeEmpty()

        // real polling: the spawned elements appear ~300ms after the click
        assertEquals(0, elements(".spawned").size())
        element("#spawn").click()
        elements(".spawned").wait(size(2))
        assertEquals(listOf("a", "b"), elements(".spawned").texts())
    }
}

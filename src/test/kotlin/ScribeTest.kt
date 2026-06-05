import logging.ConsoleScribeListener
import logging.Scribe
import logging.ScribeListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Hermetic tests for the logging layer — no browser involved. Proves the Scribe
 * dispatch pipeline that every element action flows through is actually wired
 * to listeners, and that the console listener persists failure screenshots.
 */
class ScribeTest {

    private class Recording : ScribeListener {
        val events = mutableListOf<String>()
        override fun beforeStep(name: String) { events += "before:$name" }
        override fun afterStep(name: String) { events += "after:$name" }
        override fun onFail(name: String, error: Throwable, screenshot: ByteArray?) {
            events += "fail:$name"
        }
    }

    @AfterEach
    fun cleanup() = Scribe.clearListeners()

    @Test
    fun `step dispatches before and after and returns value on success`() {
        val rec = Recording()
        Scribe.addListener(rec)

        val result = Scribe.step("do thing") { 42 }

        assertEquals(42, result)
        assertEquals(listOf("before:do thing", "after:do thing"), rec.events)
    }

    @Test
    fun `removeListener stops delivery`() {
        val rec = Recording()
        Scribe.addListener(rec)
        Scribe.removeListener(rec)

        Scribe.step("x") { }

        assertTrue(rec.events.isEmpty())
    }

    @Test
    fun `console listener writes screenshot to disk on failure`() {
        val dir = File("build/psenoid-test/${System.nanoTime()}")
        val listener = ConsoleScribeListener(saveScreenshotsOnFailure = true, screenshotDir = dir)

        listener.beforeStep("snap")
        listener.onFail("snap", RuntimeException("boom"), byteArrayOf(1, 2, 3))

        val files = dir.listFiles().orEmpty().filter { it.extension == "png" }
        assertEquals(1, files.size)
        assertArrayEquals(byteArrayOf(1, 2, 3), files.first().readBytes())
    }

    @Test
    fun `console listener tolerates a null screenshot`() {
        val dir = File("build/psenoid-test/null-${System.nanoTime()}")
        val listener = ConsoleScribeListener(saveScreenshotsOnFailure = true, screenshotDir = dir)

        listener.onFail("snap", RuntimeException("boom"), null)

        assertFalse(dir.exists())
    }
}

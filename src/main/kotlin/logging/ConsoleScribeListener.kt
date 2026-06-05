package logging

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Default [ScribeListener] that prints each step to stdout and, on failure,
 * persists the captured screenshot to disk.
 *
 * Register it via `Psenoid.enableConsoleLogging()` or
 * `Psenoid.addListener(ConsoleScribeListener(...))`.
 */
class ConsoleScribeListener(
    private val saveScreenshotsOnFailure: Boolean = true,
    private val screenshotDir: File = File("build/psenoid/screenshots")
) : ScribeListener {

    // Indent per thread so nested steps read well under parallel execution.
    private val depth = ThreadLocal.withInitial { 0 }

    override fun beforeStep(name: String) {
        log("▶ $name")
        depth.set(depth.get() + 1)
    }

    override fun afterStep(name: String) {
        depth.set((depth.get() - 1).coerceAtLeast(0))
        log("✔ $name")
    }

    override fun onFail(name: String, error: Throwable, screenshot: ByteArray?) {
        depth.set((depth.get() - 1).coerceAtLeast(0))
        log("✘ $name — ${error.message ?: error.javaClass.simpleName}")
        if (saveScreenshotsOnFailure && screenshot != null) {
            saveScreenshot(screenshot)?.let { log("  screenshot saved: ${it.absolutePath}") }
        }
    }

    private fun log(message: String) {
        println("[Psenoid] ${"  ".repeat(depth.get())}$message")
    }

    private fun saveScreenshot(bytes: ByteArray): File? = try {
        screenshotDir.mkdirs()
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))
        File(screenshotDir, "failure-$stamp.png").apply { writeBytes(bytes) }
    } catch (_: Exception) {
        null
    }
}
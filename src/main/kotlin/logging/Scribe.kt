package logging

import Psenoid

interface ScribeListener {
    fun beforeStep(name: String)
    fun afterStep(name: String)
    fun onFail(name: String, error: Throwable, screenshot: ByteArray?)
}

object Scribe {
    // Thread-safe: tests may register listeners and emit steps from parallel threads.
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<ScribeListener>()

    fun addListener(listener: ScribeListener) { listeners.add(listener) }
    fun removeListener(listener: ScribeListener) { listeners.remove(listener) }
    fun clearListeners() { listeners.clear() }

    fun <T> step(name: String, action: () -> T): T {
        listeners.forEach { it.beforeStep(name) }
        return try {
            val result = action()
            listeners.forEach { it.afterStep(name) }
            result
        } catch (e: Throwable) {
            val screenshot = try { Psenoid.engine.getScreenshotBytes() } catch (ex: Exception) { null }
            listeners.forEach { it.onFail(name, e, screenshot) }
            throw e
        }
    }
}
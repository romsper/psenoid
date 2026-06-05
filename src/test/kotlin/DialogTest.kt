import config.EngineType
import config.PsenoidConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Dialog handling is the one area where the two engines genuinely differ in
 * call ordering: Playwright registers a policy *before* the dialog opens, while
 * Selenide acts on the *already-open* native alert. Each is verified on its own.
 */
class DialogTest {

    private val fixtureUrl: String get() = FixtureServer.url

    @AfterEach
    fun tearDown() = Psenoid.close()

    private fun configure(engine: EngineType) {
        PsenoidConfig.engine = engine
        PsenoidConfig.headless = true
        PsenoidConfig.timeout = Duration.ofSeconds(10)
    }

    @Test
    fun `playwright alert confirm and prompt`() {
        configure(EngineType.PLAYWRIGHT)
        open(fixtureUrl)

        // alert: policy set before the action that opens it
        acceptAlert()
        element("#alertBtn").click()
        assertEquals("hi-alert", getAlertText())

        // confirm accepted -> true
        acceptAlert()
        element("#confirmBtn").click()
        element("#dialogResult").wait(textExact("confirmed"))

        // confirm dismissed -> false
        dismissAlert()
        element("#confirmBtn").click()
        element("#dialogResult").wait(textExact("cancelled"))

        // prompt with supplied text (this is the path that previously double-handled)
        setAlertText("Roman")
        element("#promptBtn").click()
        element("#dialogResult").wait(textExact("Roman"))
    }

    @Test
    fun `selenide alert confirm and prompt`() {
        configure(EngineType.SELENIDE)
        open(fixtureUrl)

        // alert: act on the already-open native alert
        element("#alertBtn").click()
        assertEquals("hi-alert", getAlertText())
        acceptAlert()

        // confirm accepted -> true
        element("#confirmBtn").click()
        acceptAlert()
        element("#dialogResult").wait(textExact("confirmed"))

        // confirm dismissed -> false
        element("#confirmBtn").click()
        dismissAlert()
        element("#dialogResult").wait(textExact("cancelled"))

        // prompt with supplied text
        element("#promptBtn").click()
        setAlertText("Roman")
        element("#dialogResult").wait(textExact("Roman"))
    }
}

import config.EngineType
import config.PsenoidConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class UiTest {

    @AfterEach
    fun tearDown() {
        Psenoid.close()
    }

    @Test
    fun `test selenide engine`() {
        PsenoidConfig.engine = EngineType.SELENIDE
        PsenoidConfig.headless = false

        open("https://example.com")

        element("h1").wait(visible)
        element("h1").wait(text("Example Domain"))
    }

    @Test
    fun `test playwright engine`() {
        PsenoidConfig.engine = EngineType.PLAYWRIGHT
        PsenoidConfig.headless = false

        open("https://example.com")

        element("h1").wait(visible)
        element("h1").wait(text("Example Domain"))
        element("a").click()
        element("h1").wait(visible)
    }

}
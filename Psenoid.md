# 🌐 Psenoid: Project Context & History

**Дата создания:** Апрель 2026
**Стек:** Kotlin, Playwright, Selenide, Gradle.
**Цель:** Создать open-source библиотеку (wrapper), которая объединяет мощь Playwright и Selenide под единым элегантным API в стиле Selenide.

---

## 🏗 Архитектурные решения (Ключевые договоренности)

1. **Паттерн Strategy для движка:** 
   Разработан единый интерфейс `BrowserEngine` с двумя реализациями (`PlaywrightStrategy` и `SelenideStrategy`). Переключение происходит через `OmniConfig.engine`. Поддерживается локальный и удаленный запуск (Remote/WebSocket).
2. **Ленивые элементы (Lazy Elements):** 
   Класс `WebElement` инициализируется только селектором `By`. Реальный поиск в DOM происходит только в момент вызова действия (`click`, `type`, `getText`), что исключает `StaleElementReferenceException`.
3. **Smart Waits (Умные ожидания):** 
   Вместо встроенных ассертов библиотека предоставляет метод `wait(Condition)`. Это синхронизирует состояние страницы перед действиями, не нарушая паттерн AAA (Arrange-Act-Assert).
4. **Framework Agnostic (Чистое ядро):** 
   Из библиотеки полностью исключены зависимости от Allure и Kotest/AssertJ. 
   - Логирование реализовано через кастомный `ScribeListener` (пользователи сами пишут адаптер для Allure).
   - Ассерты реализуются на стороне проекта пользователя через Kotlin Extension Functions (например, `infix fun WebElement.shouldBe(condition: Condition)`).
5. **Network Interception (Перехват сети):** 
   Реализован единый API для работы с сетью: `mockRequest`, `waitForRequest`, `waitForResponse`.
   - В Playwright используется нативный `page.route()`.
   - В Selenide настроен `BrowserUpProxy` с фильтрами запросов и ответов.
   - Поддерживается чтение заголовков (`Headers`) и сырого тела запроса/ответа (`body`).

---

## 📂 Структура проекта (Maven Central Ready)
```text

psenoid/
├── build.gradle.kts (Настроен для публикации в Maven Central: GPG подпись, Javadoc, Sources)
└── src/main/kotlin/
    ├── config/
    │   └── Config.kt (Enum EngineType, ConnectionType, таймауты)
    ├── engine/
    │   ├── BrowserEngine.kt (Интерфейс)
    │   ├── PlaywrightStrategy.kt
    │   └── SelenideStrategy.kt
    ├── logging/
    │   └── Scribe.kt (Паттерн Observer для логов и скриншотов)
    ├── network/
    │   └── NetworkTraffic.kt (Data-классы для Request, Response, MockResponse)
    ├── elements/
    │   ├── Condition.kt (visible, text)
    │   └── WebElement.kt (Реализация Smart Waits)
    └── Psenoid.kt (Глобальный фасад: функции open, element, waitForRequest и т.д.)
```

🚀 API Example (Как это выглядит для пользователя)
import io.github.romsper.psenoid.*
import io.kotest.matchers.shouldBe // Пользовательский ассерт

```
class LoginTest {
    fun `test network and UI`() {
        open("/login")
        
        // Взаимодействие с UI
        element("#user").type("admin")
        
        // Перехват сети
        val request = waitForRequest("/api/login") {
            element("#submit").click()
        }
        
        // Проверки на стороне пользователя
        request.headers["Authorization"] shouldBe "Bearer token"
        element(".dashboard").wait(visible)
    }
}
```
📌 Что делать дальше (План на будущее)
[ ] Опубликовать первую версию в Maven Central через GitHub Actions.

[ ] Добавить больше Condition (например, hidden, attribute, cssClass).

[ ] Добавить класс WebCollection (аналог ElementsCollection в Selenide) для работы со списками элементов (elements(".item")).

[ ] Рассмотреть добавление AppiumStrategy для поддержки мобильного тестирования.

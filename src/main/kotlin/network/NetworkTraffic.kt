package network

data class MockResponse(
    val status: Int = 200,
    val body: String,
    val contentType: String = "application/json"
)

data class NetworkRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String
)

data class NetworkResponse(
    val url: String,
    val status: Int,
    val headers: Map<String, String>,
    val body: String
)
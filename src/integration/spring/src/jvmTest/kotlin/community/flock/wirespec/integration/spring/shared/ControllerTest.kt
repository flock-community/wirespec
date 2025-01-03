import community.flock.wirespec.integration.spring.shared.extractQueries
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QueryParameterExtractorTest {

    @Test
    fun `should handle ampersand-separated parameters`() {
        val query = "tags=Smilodon%20Rex&tags=Dodo%20Bird&tags=Mammoth"

        assertEquals(
            mapOf("tags" to listOf("Smilodon Rex", "Dodo Bird", "Mammoth")),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle comma-separated values`() {
        val query = "tags=Smilodon%20Rex,Dodo%20Bird,Mammoth"

        assertEquals(
            mapOf("tags" to listOf("Smilodon Rex", "Dodo Bird", "Mammoth")),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle mixed separator format`() {
        val query = "tags=Smilodon%20Rex,Dodo%20Bird&tags=Mammoth"

        assertEquals(
            mapOf("tags" to listOf("Smilodon Rex", "Dodo Bird", "Mammoth")),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle multiple different parameters`() {
        val query = "tags=Big%20Cat,Small%20Dog&color=deep%20red&size=very%20large"

        assertEquals(
            mapOf(
                "tags" to listOf("Big Cat", "Small Dog"),
                "color" to listOf("deep red"),
                "size" to listOf("very large")
            ),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle values containing equals sign`() {
        val query = "equation=1%2B1%3D2&url=http%3A%2F%2Fexample.com%3Fa%3Db"

        assertEquals(
            mapOf(
                "equation" to listOf("1+1=2"),
                "url" to listOf("http://example.com?a=b")
            ),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle special characters`() {
        val query = "text=%21%40%23%24%25%5E%26*%28%29&name=John+Doe"

        assertEquals(
            mapOf(
                "text" to listOf("!@#$%^&*()"),
                "name" to listOf("John Doe")
            ),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle plus signs in query parameters`() {
        val query = "name=John+Doe&title=Senior+Software+Engineer"

        assertEquals(
            mapOf(
                "name" to listOf("John Doe"),
                "title" to listOf("Senior Software Engineer")
            ),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle parameters with empty values`() {
        val query = "empty=&next=value"

        assertEquals(
            mapOf(
                "empty" to listOf(""),
                "next" to listOf("value")
            ),
            extractQueries(query)
        )
    }

    @Test
    fun `should handle unicode characters`() {
        val query = "text=%F0%9F%98%8A&name=%E6%97%A5%E6%9C%AC"

        assertEquals(
            mapOf(
                "text" to listOf("😊"),
                "name" to listOf("日本")
            ),
            extractQueries(query)
        )
    }
}
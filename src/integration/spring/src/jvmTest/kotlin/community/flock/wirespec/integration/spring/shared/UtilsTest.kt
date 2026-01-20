package community.flock.wirespec.integration.spring.shared

import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun `should extract path from pathInfo`() {
        val request = MockHttpServletRequest().apply {
            pathInfo = "/pet/1"
        }
        val result = request.extractPath()
        assertEquals(listOf("pet", "1"), result)
    }

    @Test
    fun `should extract path from servletPath when pathInfo is null`() {
        val request = MockHttpServletRequest().apply {
            servletPath = "/pet/1"
            pathInfo = null
        }
        val result = request.extractPath()
        assertEquals(listOf("pet", "1"), result)
    }

    @Test
    fun `should handle empty path`() {
        val request = MockHttpServletRequest().apply {
            pathInfo = "/"
        }
        val result = request.extractPath()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `should extract queries from queryString`() {
        val queryString = "name=dog&status=available,pending&encoded=%20"
        val result = extractQueries(queryString)
        val expected = mapOf(
            "name" to listOf("dog"),
            "status" to listOf("available", "pending"),
            "encoded" to listOf(" "),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should handle empty query value`() {
        val queryString = "name="
        val result = extractQueries(queryString)
        val expected = mapOf(
            "name" to listOf(""),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should handle multiple same keys`() {
        val queryString = "name=dog&name=cat"
        val result = extractQueries(queryString)
        val expected = mapOf(
            "name" to listOf("dog", "cat"),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should return empty map for null queryString`() {
        val result = extractQueries(null)
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `should filter not empty queries`() {
        val queries = mapOf(
            "name" to listOf("dog"),
            "empty" to emptyList(),
            "status" to listOf("available"),
        )
        val result = queries.filterNotEmpty()
        val expected = mapOf(
            "name" to listOf("dog"),
            "status" to listOf("available"),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should extract queries from HttpServletRequest`() {
        val request = MockHttpServletRequest().apply {
            queryString = "name=dog"
        }
        val result = request.extractQueries()
        assertEquals(mapOf("name" to listOf("dog")), result)
    }
}

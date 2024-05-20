package community.flock.wirespec.generator

import arrow.core.getOrElse
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


private val src = """
        type UUID /^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}${'$'}/g
        
        type Todo {
          id: UUID,
          name: String,
          done: Boolean
        }
        
        endpoint GetTodos GET /todos -> {
            200 -> Todo[]
        }
        
        endpoint GetTodosById GET Todo /todos/{id: UUID} -> {
            200 -> Todo
            404 -> Error
        }
        
        endpoint GetTodosByIdAndString GET Todo /todos/{id: UUID}/{string:String} -> {
            200 -> Todo
            404 -> Error
        }
        
    """.trimIndent()

class RouterTest {

    private fun parser(source: String) = WirespecSpec
        .parse(source)(noLogger)
        .getOrElse { e -> error("Cannot parse: ${e.map { it.message }}") }

    private val router = parser(src).router()

    @Test
    fun testGetTodos() {
        val res = router.match(Endpoint.Method.GET, "/todos")
        assertEquals("GetTodos", res?.endpoint?.name)
        assertEquals(emptyMap(), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosSlash() {
        val res = router.match(Endpoint.Method.GET, "/todos/")
        assertEquals("GetTodos", res?.endpoint?.name)
        assertEquals(emptyMap(), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosById() {
        val res = router.match(Endpoint.Method.GET, "/todos/123")
        assertEquals("GetTodosById", res?.endpoint?.name)
        assertEquals(mapOf("id" to "123"), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosByIdSlash() {
        val res = router.match(Endpoint.Method.GET, "/todos/123/")
        assertEquals("GetTodosById", res?.endpoint?.name)
        assertEquals(mapOf("id" to "123"), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosByIdAndString() {
        val res = router.match(Endpoint.Method.GET, "/todos/123/hello")
        assertEquals("GetTodosByIdAndString", res?.endpoint?.name)
        assertEquals(mapOf("id" to "123", "string" to "hello"), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosByIdAndStringSlash() {
        val res = router.match(Endpoint.Method.GET, "/todos/123/hello/")
        assertEquals("GetTodosByIdAndString", res?.endpoint?.name)
        assertEquals(mapOf("id" to "123", "string" to "hello"), res?.params)
        assertEquals(emptyMap(), res?.query)
    }

    @Test
    fun testGetTodosByIdAndStringQuery() {
        val res = router.match(Endpoint.Method.GET, "/todos/123/hello?foo=bar")
        assertEquals("GetTodosByIdAndString", res?.endpoint?.name)
        assertEquals(mapOf("id" to "123", "string" to "hello"), res?.params)
        assertEquals(mapOf("foo" to "bar"), res?.query)
    }


    @Test
    fun testNoMatch() {
        val res = router.match(Endpoint.Method.GET, "/hello/world")
        assertNull(res)
    }
}
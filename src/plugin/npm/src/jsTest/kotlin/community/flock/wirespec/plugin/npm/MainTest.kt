package community.flock.wirespec.plugin.npm

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.NoLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MainTest {

    private val personWs =
        // language=ws
        """
        type TodoIdentifier /^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}${'$'}/g
        type Name /^[0-9a-zA-Z]{1,50}${'$'}/g
        type DutchPostalCode /^([0-9]{4}[A-Z]{2})${'$'}/g
        type Date /^([0-9]{2}-[0-9]{2}-20[0-9]{2})${'$'}/g

        type Address {
          street: Name,
          houseNumber: Integer,
          postalCode: DutchPostalCode
        }

        type Person {
          firstname: Name,
          lastName: Name,
          age: Integer,
          address: Address
        }

        type Todo {
          id: TodoIdentifier,
          person: Person,
          done: Boolean,
          prio: Integer,
          date: Date
        }

        type Error {
          reason: String
        }

        endpoint GetTodos GET /todos -> {
            200 -> Todo[]
        }

        endpoint PostTodo POST Todo /todos -> {
            200 -> Todo
        }

        endpoint PutTodo PUT Todo /todos/{id: TodoIdentifier} -> {
            200 -> Todo
            404 -> Error
        }

        endpoint DeleteTodo DELETE /todos/{id: TodoIdentifier} -> {
            200 -> Todo
            404 -> Error
        }
        """.trimIndent()

    @Test
    fun testEmit() {
        val res = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent("", personWs))).getOrNull()
        assertNotNull(res)
        val openApiV2 = emit(res.produce(), Emitters.OPENAPI_V2, "")
        val openApiV3 = emit(res.produce(), Emitters.OPENAPI_V3, "")
        assertEquals("""{"swagger":"2.0"""", openApiV2.first().result.substring(0, 16))
        assertEquals("""{"openapi":"3.0.0"""", openApiV3.first().result.substring(0, 18))
    }
}

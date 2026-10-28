package community.flock.wirespec.compiler.core.ir.transformer

import community.flock.wirespec.compiler.core.ir.Assignment
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.ir.struct
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedTransformsTest {

    private val response = struct("Response404") {
        field("status", Type.IntegerLiteral(404), isOverride = true)
        field("body", Type.String, isOverride = true)
        constructo {
            arg("body", Type.String)
            assign("status", Literal(404, Type.Integer()))
            assign("body", VariableReference(Name.of("body")))
        }
    }

    @Test
    fun pinResponseStatusFixesTheStatusAsAMember() {
        val result = response.pinResponseStatus()

        assertEquals(listOf(Name.of("body")), result.fieldList().map { it.name })
        assertEquals(
            listOf(Field(Name.of("status"), Type.IntegerLiteral(404), isOverride = true, initializer = Literal(404, Type.Integer()))),
            result.elements,
        )
        assertEquals(
            listOf(Name.of("body")),
            result.constructors.single().body.filterIsInstance<Assignment>().map { it.name },
        )
    }

    @Test
    fun pinResponseStatusLeavesAStructWithoutALiteralFieldAsIs() {
        val struct = struct("Todo") {
            field("status", Type.Integer())
        }

        assertEquals(struct, struct.pinResponseStatus())
    }
}

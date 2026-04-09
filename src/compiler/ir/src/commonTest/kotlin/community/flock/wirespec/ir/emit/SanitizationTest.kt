package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.transformChildren
import kotlin.test.Test
import kotlin.test.assertEquals

class SanitizationTest {

    private val javaLikeConfig = SanitizationConfig(
        reservedKeywords = setOf("class", "return", "import"),
        escapeKeyword = { "_$it" },
        fieldNameCase = { name ->
            val sanitized = if (name.parts.size > 1) name.camelCase() else name.value()
            Name(listOf(sanitized))
        },
        parameterNameCase = { name ->
            Name(listOf(name.camelCase()))
        },
        sanitizeSymbol = { str ->
            str.split(".", " ", "-")
                .mapIndexed { index, s ->
                    if (index > 0) s.replaceFirstChar { it.uppercase() } else s
                }
                .joinToString("")
                .filter { it.isLetterOrDigit() || it == '_' }
        },
    )

    @Test
    fun sanitizeFieldNames() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("first-name"), Type.String),
                Field(Name.of("last.name"), Type.String),
            ),
        )

        val result = struct.sanitizeNames(javaLikeConfig)

        assertEquals("firstName", result.fields[0].name.value())
        assertEquals("lastName", result.fields[1].name.value())
    }

    @Test
    fun sanitizeReservedKeywordField() {
        val struct = Struct(
            name = Name.of("Item"),
            fields = listOf(
                Field(Name.of("class"), Type.String),
            ),
        )

        val result = struct.sanitizeNames(javaLikeConfig)

        assertEquals("_class", result.fields[0].name.value())
    }

    @Test
    fun sanitizeParameterNames() {
        val fn = Function(
            name = Name.of("doSomething"),
            parameters = listOf(
                Parameter(Name.of("my-param"), Type.String),
            ),
            returnType = Type.String,
            body = listOf(ReturnStatement(VariableReference(Name.of("my-param")))),
        )

        val result = fn.sanitizeNames(javaLikeConfig)

        assertEquals("myParam", result.parameters[0].name.value())
    }

    @Test
    fun sanitizeFieldCallExpressions() {
        val fn = Function(
            name = Name.of("getValue"),
            parameters = emptyList(),
            returnType = Type.String,
            body = listOf(
                ReturnStatement(
                    FieldCall(
                        receiver = VariableReference(Name.of("obj")),
                        field = Name.of("field-name"),
                    ),
                ),
            ),
        )

        val result = fn.sanitizeNames(javaLikeConfig)

        val returnStmt = result.body[0] as ReturnStatement
        val fieldCall = returnStmt.expression as FieldCall
        assertEquals("fieldName", fieldCall.field.value())
    }

    @Test
    fun sanitizeWithExtraStatementTransform() {
        val configWithExtra = javaLikeConfig.copy(
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is VariableReference -> VariableReference(
                        name = Name(listOf(stmt.name.camelCase())),
                    )
                    else -> stmt.transformChildren(tr)
                }
            },
        )

        val fn = Function(
            name = Name.of("test"),
            parameters = emptyList(),
            returnType = Type.String,
            body = listOf(ReturnStatement(VariableReference(Name.of("some-var")))),
        )

        val result = fn.sanitizeNames(configWithExtra)

        val returnStmt = result.body[0] as ReturnStatement
        val varRef = returnStmt.expression as VariableReference
        assertEquals("someVar", varRef.name.value())
    }
}

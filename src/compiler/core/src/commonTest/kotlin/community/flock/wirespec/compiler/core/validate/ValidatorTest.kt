package community.flock.wirespec.compiler.core.validate

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.ValidationContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.validate.Validator.validate
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ValidatorTest {

    private fun parser(source1: String, source2: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(
        nonEmptyListOf(
            ModuleContent("module1.ws", source1),
            ModuleContent("module2.ws", source2),
        ),
    )

    private val validator = object : ValidationContext, NoLogger {
        override val spec = WirespecSpec
    }

    @Test
    fun testDuplicateEndpointNamesAcrossModules() {
        val source1 = """
            |endpoint Test GET /Test1 -> {
            |    200 -> String
            |}
        """.trimMargin()

        val source2 = """
            |endpoint Test GET /Test2 -> {
            |    200 -> String
            |}
        """.trimMargin()

        val ast = parser(source1, source2)

        validator.validate(ast)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateEndpointError>()
    }

    @Test
    fun testNoDuplicateEndpointNamesAcrossModules() {
        val source1 = """
            |endpoint Test1 GET /Test1 -> {
            |    200 -> String
            |}
        """.trimMargin()

        val source2 = """
            |endpoint Test2 GET /Test2 -> {
            |    200 -> String
            |}
        """.trimMargin()

        val ast = parser(source1, source2)

        validator.validate(ast)
            .shouldBeRight()
    }
}

package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ValidationContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.validate
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ValidatorTest {

    private fun validate(vararg sources: String): EitherNel<WirespecException, AST> {
        val context = object : ValidationContext, NoLogger {
            override val spec = WirespecSpec
        }

        return context.validate(
            context.parse(
                sources.map { ModuleContent("module.ws", it) }.toNonEmptyListOrNull()
                    ?: throw IndexOutOfBoundsException("No sources provided"),
            ),
        )
    }

    @Test
    fun duplicateEndpointSameFile() {
        val source = """
            |endpoint Test GET /Test1 -> {
            |    200 -> String
            |}
            |
            |endpoint Test GET /Test2 -> {
            |    200 -> String
            |}
        """.trimMargin()

        validate(source)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateEndpointError>()
    }

    @Test
    fun duplicateEndpointDifferentFile() {
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

        validate(source1, source2)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateEndpointError>()
    }
}

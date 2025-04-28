package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.DuplicateChannelError
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.DuplicateTypeError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
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
    fun uniqueEndpoints() {
        val source = """
            |endpoint Test1 GET /Test1 -> {
            |    200 -> String
            |}
            |
            |endpoint Test2 GET /Test2 -> {
            |    200 -> String
            |}
        """.trimMargin()

        validate(source)
            .shouldBeRight()
    }

    @Test
    fun uniqueTypes() {
        val source = """
            |type Foo { str: String }
            |type Bar { str: String }
        """.trimMargin()

        validate(source)
            .shouldBeRight()
    }

    @Test
    fun uniqueChannels() {
        val source = """
            |channel Foo -> String
            |channel Bar -> String
        """.trimMargin()

        validate(source)
            .shouldBeRight()
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

    @Test
    fun duplicateTypeDifferentFile() {
        val source1 = """
           |type Foo { str: String }
        """.trimMargin()

        val source2 = """
            |type Foo { str: String }
        """.trimMargin()

        validate(source1, source2)
            .shouldBeRight()
    }

    @Test
    fun duplicateTypeSameFile() {
        val source = """
            |type Foo { str: String }
            |type Foo { str: String }
        """.trimMargin()

        validate(source)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateTypeError>()
    }

    @Test
    fun multipleTypeViolations() {
        val source = """
            |type Foo { str: String }
            |type Foo { str: String }
            |type Foo { str: String }
        """.trimMargin()

        validate(source)
            .shouldBeLeft()
            .size shouldBe 3
    }

    @Test
    fun multipleEndpointViolations() {
        val source1 = """
            |endpoint Test GET /Test1 -> {
            |    200 -> String
            |}
            |
            |endpoint Test GET /Test2 -> {
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
            .size shouldBe 3
    }

    @Test
    fun severalErrorTypes() {
        val source = """
            |type Foo { str: String }
            |type Foo { str: String }
            |
            |channel Foo -> String
            |channel Foo -> String
            |
            |endpoint Test GET /Test1 -> {
            |    200 -> String
            |}
            |
            |endpoint Test GET /Test2 -> {
            |    200 -> String
            |}
        """.trimMargin()

        validate(source).shouldBeLeft()
            .all.map { it::class }
            .shouldContainAll(
                listOf(
                    DuplicateTypeError::class,
                    DuplicateEndpointError::class,
                    DuplicateChannelError::class,
                ),
            )
    }

    @Test
    fun duplicateChannelSameFile() {
        val source = """
            |channel Foo -> String
            |channel Foo -> String
        """.trimMargin()

        validate(source)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateChannelError>()
    }

    @Test
    fun duplicateChannelDifferentFile() {
        val source1 = """
            |channel Foo -> String
        """.trimMargin()

        val source2 = """
            |channel Foo -> String
        """.trimMargin()

        validate(source1, source2)
            .shouldBeLeft()
            .head.shouldBeInstanceOf<DuplicateChannelError>()
    }

    @Test
    fun multipleChannelViolations() {
        val source1 = """
            |channel Foo -> String
            |channel Foo -> String
        """.trimMargin()

        val source2 = """
            |channel Foo -> String
        """.trimMargin()

        validate(source1, source2)
            .shouldBeLeft()
            .size shouldBe 3
    }
}

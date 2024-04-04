package community.flock.wirespec.compiler.core.parse

import arrow.core.flatMap
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParserValidatorTest {

    private fun parser() = Parser(object : Logger(false) {})
    private fun compile(source: String) = Wirespec.tokenize(source)
        .let(parser()::parse)
        .flatMap { it.validate() }

    @Test
    fun shouldHaveSelfRef() {
        val source = """
            type Self {
              self: Self
            }

        """.trimIndent()

        compile(source)
            .shouldBeRight()
    }

    @Test
    fun shouldNotFindReferenceInType() {
        val source = """
            type Foo {
              bar: Bar
            }
        """.trimIndent()

        compile(source)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .run {
                message shouldBe "Cannot find reference: Bar"
                coordinates.line shouldBe 2
                coordinates.position shouldBe 11
                coordinates.idxAndLength.idx shouldBe 21
                coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEndpoint() {
        val source = """
            endpoint FooPoint POST Foo /foo -> {
              200 -> Bar
            }
        """.trimIndent()

        compile(source)
            .shouldBeLeft()
            .apply { size shouldBe 2 }
            .apply {
                get(0).message shouldBe "Cannot find reference: Foo"
                get(0).coordinates.line shouldBe 1
                get(0).coordinates.position shouldBe 27
                get(0).coordinates.idxAndLength.idx shouldBe 26
                get(0).coordinates.idxAndLength.length shouldBe 3
            }
            .apply {
                get(1).message shouldBe "Cannot find reference: Bar"
                get(1).coordinates.line shouldBe 2
                get(1).coordinates.position shouldBe 13
                get(1).coordinates.idxAndLength.idx shouldBe 49
                get(1).coordinates.idxAndLength.length shouldBe 3
            }
    }
}
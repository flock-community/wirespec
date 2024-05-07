package community.flock.wirespec.compare

import Compare
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test

class CompareTest {

    fun String.parse() = WirespecSpec.parse(this)(noLogger).getOrNull()

    @Test
    fun test(){
        val left = """
            |type Foo {
            |   bar: String,
            |   baz: Number
            |}
        """.trimMargin()

        val right = """
            |type Foo {
            |   bar: String,
            |   baz: Number
            |}
        """.trimMargin()

        Compare.compare(left.parse(), right.parse())
    }
}
package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.common.assertValid
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import kotlin.test.Test

class CompileToWirespecTest {

    private val logger = TestLogger

    @Test
    fun testCompileType() {
        val source = """
                type Bla {
                  foo: String,
                  bar: String
                }
                
                """.trimIndent()

        Wirespec.compile(source)(logger)(WirespecEmitter(logger = logger))
            .map { it.first().second }
            .onLeft(::println)
            .assertValid(source)
    }

    @Test
    fun testCompileRefined() {
        val source = """
                refined Name /^[a-zA-Z]{1,50}$/g
                
                """.trimIndent()

        Wirespec.compile(source)(logger)(WirespecEmitter(logger = logger))
            .map { it.first().second }
            .onLeft(::println)
            .assertValid(source)
    }
}

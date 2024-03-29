package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileToWirespecTest {

    private val logger = noLogger

    @Test
    fun testCompileType() {
        val source = """
                type Bla {
                  foo: String,
                  bar: String
                }
          
                """.trimIndent()

        Wirespec.compile(source)(logger)(WirespecEmitter(logger = logger))
            .map { it.first().result } shouldBeRight source
    }

    @Test
    fun testCompileRefined() {
        val source = """
                refined Name /^[a-zA-Z]{1,50}$/g
                
                """.trimIndent()

        Wirespec.compile(source)(logger)(WirespecEmitter(logger = logger))
            .map { it.first().result } shouldBeRight source
    }
}

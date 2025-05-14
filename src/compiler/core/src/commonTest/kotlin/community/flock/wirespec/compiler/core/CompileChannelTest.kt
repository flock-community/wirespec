package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileChannelTest {

    private val compiler = """
        |channel Queue -> String
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated.channel
            |
            |interface QueueChannel {
            |   operator fun invoke(message: String)
            |}
            |
        """.trimMargin()

        compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated.channel;
            |
            |
            |
            |public interface Queue {
            |   void invoke(String message);
            |}
            |
        """.trimMargin()

        compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun typeScript() {
        val ts = """
            |// TODO("Not yet implemented")
            |
        """.trimMargin()

        compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun wirespec() {
        val wirespec = """
            |channel Queue -> String
        """.trimMargin()

        compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}

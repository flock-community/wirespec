package community.flock.wirespec.emitters.scala

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class SealedHierarchiesTest {

    @Test
    fun writesAUnionAndItsMembersInOneFile() {
        val files = """
            |type Password { password: String }
            |type Token { token: String }
            |type Account = Password | Token
            |type User { name: String }
        """.trimMargin().emit()

        files.keys.filter { it.startsWith("model/") } shouldContainExactly listOf("model/Account.scala", "model/User.scala")
        files.getValue("model/Account.scala").let { account ->
            account shouldContain "sealed trait Account"
            account shouldContain "case class Password("
            account shouldContain "case class Token("
            Regex("^package ", RegexOption.MULTILINE).findAll(account).count() shouldBe 1
        }
    }

    @Test
    fun writesUnionsThatShareAMemberInOneFile() {
        val files = """
            |type A { a: String }
            |type B { b: String }
            |type C { c: String }
            |type X = A | B
            |type Y = B | C
        """.trimMargin().emit()

        files.keys.filter { it.startsWith("model/") } shouldContainExactly listOf("model/X.scala")
        files.getValue("model/X.scala").let { hierarchy ->
            listOf("sealed trait X", "sealed trait Y", "case class A(", "case class B(", "case class C(").forEach { hierarchy shouldContain it }
        }
    }

    private fun String.emit(): Map<String, String> = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), this)))
        .shouldBeRight()
        .let { ScalaEmitter(emitShared = EmitShared(false)).emit(it, noLogger) }
        .associate { it.file.substringAfter("generated/") to it.result }
}

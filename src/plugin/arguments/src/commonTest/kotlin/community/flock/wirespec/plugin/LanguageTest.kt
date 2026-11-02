package community.flock.wirespec.plugin

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.common.ConverterContext
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LanguageTest {
    @Test
    fun testLanguages() {
        Language.toString() shouldBe "Java, Kotlin, TypeScript, Python, Rust, Scala, Wirespec, OpenAPIV2, OpenAPIV3, Avro, AvroIdl, GraphQL, AsyncAPI"
    }

    @Test
    fun theConverterLanguagesNameTheirFileAfterTheContext() {
        val ast = object : ParseContext, NoLogger {}
            .parse(nonEmptyListOf(ModuleContent(FileUri("todo.ws"), "type Todo { name: String }")))
            .shouldBeRight()

        listOf(Language.OpenAPIV2, Language.OpenAPIV3, Language.Avro, Language.AvroIdl, Language.GraphQL, Language.AsyncAPI)
            .map { it.toEmitter(PackageName("todo"), EmitShared(), ConverterContext(name = "Todos", version = "1.2.3")) }
            .map { it.emit(ast, noLogger).single().file }
            .shouldContainExactly("Todos.json", "Todos.json", "Todos.avsc", "Todos.avdl", "Todos.graphql", "Todos.json")
    }
}

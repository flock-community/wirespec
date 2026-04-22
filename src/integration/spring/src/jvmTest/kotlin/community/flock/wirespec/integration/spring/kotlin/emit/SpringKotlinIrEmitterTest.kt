package community.flock.wirespec.integration.spring.kotlin.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SpringKotlinIrEmitterTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    @Test
    fun `Should add Spring mapping annotations to handler methods`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val emitted = SpringKotlinIrEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)

        val postEndpoint = emitted.single { it.file.endsWith("RequestParrot.kt") }.result
        val getEndpoint = emitted.single { it.file.endsWith("GetTodos.kt") }.result
        val patchEndpoint = emitted.single { it.file.endsWith("PatchTodos.kt") }.result

        assertContains(postEndpoint, """@org.springframework.web.bind.annotation.PostMapping("/api/parrot")""")
        assertContains(getEndpoint, """@org.springframework.web.bind.annotation.GetMapping("/api/todos")""")
        assertContains(patchEndpoint, """@org.springframework.web.bind.annotation.PatchMapping("/api/todos/{id}")""")
    }

    @Test
    fun `Should emit a WirespecNativeHints file with registrations for all models and endpoints`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val emitted = SpringKotlinIrEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)

        val hints = emitted.single { it.file.endsWith("WirespecNativeHints.kt") }

        assertEquals("community/flock/wirespec/spring/test/WirespecNativeHints.kt", hints.file)

        val expectedSnippets = listOf(
            "package community.flock.wirespec.spring.test",
            "import community.flock.wirespec.spring.test.model.TodoId",
            "import community.flock.wirespec.spring.test.endpoint.RequestParrot",
            "import org.springframework.aot.hint.RuntimeHintsRegistrar",
            "import org.springframework.context.annotation.Configuration",
            "@Configuration",
            "@ImportRuntimeHints(WirespecNativeHints.GeneratedHints::class)",
            "open class WirespecNativeHints {",
            "class GeneratedHints : RuntimeHintsRegistrar {",
            "registerWithInnerClasses(hints, TodoId::class.java, allMembers)",
            "registerWithInnerClasses(hints, RequestBodyParrot::class.java, allMembers)",
            "registerWithInnerClasses(hints, RequestParrot::class.java, allMembers)",
            "registerWithInnerClasses(hints, GetTodos::class.java, allMembers)",
            "registerWithInnerClasses(hints, PatchTodos::class.java, allMembers)",
            "for (inner in clazz.declaredClasses) {",
        )
        expectedSnippets.forEach { snippet -> assertContains(hints.result, snippet) }
    }
}

package community.flock.wirespec.integration.spring.java.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.integration.spring.emit.SpringMappingAnnotationsSupportExtension
import community.flock.wirespec.integration.spring.emit.SpringMappingNativeSupportExtension
import community.flock.wirespec.ir.extension.applyExtensions
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SpringJavaIrExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun springEmitter(packageName: PackageName) = JavaIrEmitter(packageName, EmitShared(false))
        .applyExtensions(
            listOf(
                SpringMappingAnnotationsSupportExtension(FileExtension.Java),
                SpringMappingNativeSupportExtension(packageName, FileExtension.Java),
            ),
        )

    @Test
    fun `Should add Spring mapping annotations to handler methods`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val emitted = springEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)

        val postEndpoint = emitted.single { it.file.endsWith("RequestParrot.java") }.result
        val getEndpoint = emitted.single { it.file.endsWith("GetTodos.java") }.result
        val patchEndpoint = emitted.single { it.file.endsWith("PatchTodos.java") }.result

        assertContains(postEndpoint, """@org.springframework.web.bind.annotation.PostMapping("/api/parrot")""")
        assertContains(getEndpoint, """@org.springframework.web.bind.annotation.GetMapping("/api/todos")""")
        assertContains(patchEndpoint, """@org.springframework.web.bind.annotation.PatchMapping("/api/todos/{id}")""")
    }

    @Test
    fun `Should emit a WirespecNativeHints file with registrations for all models and endpoints`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val emitted = springEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)

        val hints = emitted.single { it.file.endsWith("WirespecNativeHints.java") }

        assertEquals("community/flock/wirespec/spring/test/WirespecNativeHints.java", hints.file)

        val expectedSnippets = listOf(
            "package community.flock.wirespec.spring.test;",
            "import community.flock.wirespec.spring.test.model.TodoId;",
            "import community.flock.wirespec.spring.test.endpoint.RequestParrot;",
            "import org.springframework.aot.hint.RuntimeHintsRegistrar;",
            "import org.springframework.context.annotation.Configuration;",
            "@Configuration",
            "@ImportRuntimeHints(WirespecNativeHints.GeneratedHints.class)",
            "public class WirespecNativeHints {",
            "static class GeneratedHints implements RuntimeHintsRegistrar {",
            "registerWithInnerClasses(hints, TodoId.class, allMembers);",
            "registerWithInnerClasses(hints, RequestBodyParrot.class, allMembers);",
            "registerWithInnerClasses(hints, RequestParrot.class, allMembers);",
            "registerWithInnerClasses(hints, GetTodos.class, allMembers);",
            "registerWithInnerClasses(hints, PatchTodos.class, allMembers);",
            "for (Class<?> inner : clazz.getDeclaredClasses()) {",
        )
        expectedSnippets.forEach { snippet -> assertContains(hints.result, snippet) }
    }
}

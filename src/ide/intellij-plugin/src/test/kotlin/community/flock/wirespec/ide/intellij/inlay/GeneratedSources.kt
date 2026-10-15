package community.flock.wirespec.ide.intellij.inlay

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.utils.NoLogger
import junit.framework.TestCase.assertEquals

private val todoSpec = """
    |endpoint GetTodoById GET /todos/{id: String} -> {
    |    200 -> TodoDto
    |}
    |type TodoDto {
    |    description: String
    |}
""".trimMargin()

internal fun CodeInsightTestFixture.addTodoEndpoint(emitter: Emitter) {
    addFileToProject("todo.ws", todoSpec)
    object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter)
    }.compile(nonEmptyListOf(ModuleContent(FileUri("todo.ws"), todoSpec)))
        .getOrElse { errors -> error("Wirespec compilation failed: $errors") }
        .forEach { addFileToProject(it.file, it.result) }
}

internal fun DaemonBoundCodeVisionProvider.entries(fixture: CodeInsightTestFixture): List<ClickableTextCodeVisionEntry> = computeForEditor(fixture.editor, fixture.file)
    .map { (_, entry) -> entry as ClickableTextCodeVisionEntry }

internal fun CodeInsightTestFixture.assertNavigatedToTodoEndpoint() {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: error("No editor opened")
    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: error("No file opened")
    assertEquals("todo.ws", file.name)
    assertEquals(todoSpec.indexOf("GetTodoById"), editor.caretModel.offset)
}

package community.flock.wirespec.ide.intellij

import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementDef

class ReferenceTest : BasePlatformTestCase() {

    fun testResolvesTypeReferenceToItsDefinitionInAnotherFile() {
        myFixture.addFileToProject("todo.ws", "type TodoDto {\n    description: String\n}")
        myFixture.configureByText("endpoint.ws", "endpoint GetTodos GET /todos -> {\n    200 -> Todo<caret>Dto[]\n}")

        val definition = myFixture.getReferenceAtCaretPositionWithAssertion().resolve()

        assertInstanceOf(definition, CustomTypeElementDef::class.java)
        assertEquals("TodoDto", definition?.text)
        assertEquals("todo.ws", definition?.containingFile?.name)
    }

    fun testFindsUsagesOfATypeAcrossFiles() {
        myFixture.addFileToProject("endpoint.ws", "endpoint GetTodos GET /todos -> {\n    200 -> TodoDto[]\n}")
        myFixture.addFileToProject("list.ws", "type TodoList {\n    todos: TodoDto[]\n}")
        myFixture.configureByText("todo.ws", "type Todo<caret>Dto {\n    description: String\n}")

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSameElements(usages.map { it.file?.name }, "endpoint.ws", "list.ws")
    }

    fun testRenamesATypeAndItsReferences() {
        myFixture.addFileToProject("endpoint.ws", "endpoint GetTodos GET /todos -> {\n    200 -> TodoDto[]\n}")
        myFixture.configureByText("todo.ws", "type Todo<caret>Dto {\n    description: String\n}")

        myFixture.renameElementAtCaret("Todo")

        myFixture.checkResult("type Todo {\n    description: String\n}")
        myFixture.checkResult("endpoint.ws", "endpoint GetTodos GET /todos -> {\n    200 -> Todo[]\n}", false)
    }

    fun testOffersInPlaceRenameForTypes() {
        myFixture.configureByText("todo.ws", "type Todo<caret>Dto {\n    description: String\n}")

        val refactoringSupport = LanguageRefactoringSupport.getInstance().forLanguage(Language)

        assertTrue(refactoringSupport.isMemberInplaceRenameAvailable(myFixture.elementAtCaret, null))
    }
}

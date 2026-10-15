package community.flock.wirespec.ide.intellij

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AnnotatorTest : BasePlatformTestCase() {

    fun testReportsSyntaxErrorOnTheOffendingToken() {
        myFixture.configureByText("todo.ws", "type TodoDto {\n    description String\n}")

        val error = myFixture.doHighlighting(HighlightSeverity.ERROR).single()

        assertEquals("String", error.text)
        assertTrue(error.description, error.description.startsWith("Colon expected"))
    }

    fun testReportsUnknownTypeReference() {
        myFixture.configureByText("todo.ws", "type TodoDto {\n    owner: User\n}")

        val error = myFixture.doHighlighting(HighlightSeverity.ERROR).single()

        assertEquals("User", error.text)
        assertEquals("Cannot find reference: User", error.description)
    }

    fun testResolvesTypesDefinedInAnotherFileOfTheSameDirectory() {
        myFixture.addFileToProject("user.ws", "type User {\n    name: String\n}")
        myFixture.configureByText("todo.ws", "type TodoDto {\n    owner: User\n}")

        assertEmpty(myFixture.doHighlighting(HighlightSeverity.ERROR))
    }
}

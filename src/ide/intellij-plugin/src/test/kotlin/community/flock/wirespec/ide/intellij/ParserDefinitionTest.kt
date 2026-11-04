package community.flock.wirespec.ide.intellij

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ParserDefinitionTest : BasePlatformTestCase() {

    fun testFileNodeTypeIsASingleton() {
        val definition = LanguageParserDefinitions.INSTANCE.forLanguage(Language)

        assertSame(definition.fileNodeType, definition.fileNodeType)
    }

    fun testParsingFilesDoesNotRegisterNewElementTypes() {
        myFixture.configureByText("warmup.ws", "type TodoDto { description: String }")
        val registered = wirespecElementTypeCount()

        repeat(10) { myFixture.configureByText("todo$it.ws", "type TodoDto { description: String }") }

        assertEquals(registered, wirespecElementTypeCount())
    }

    private fun wirespecElementTypeCount(): Int = IElementType.enumerate { it.language == Language }.size
}

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
        val registered = IElementType.getAllocatedTypesCount()

        repeat(10) { myFixture.configureByText("todo$it.ws", "type TodoDto { description: String }") }

        assertEquals(registered, IElementType.getAllocatedTypesCount())
    }
}

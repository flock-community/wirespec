package community.flock.wirespec.ide.intellij

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementDef
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementRef
import community.flock.wirespec.ide.intellij.parser.EndpointDefElement
import community.flock.wirespec.ide.intellij.parser.Parser

class ParserTest : BasePlatformTestCase() {

    fun testWsFilesUseTheWirespecFileType() {
        val file = myFixture.configureByText("todo.ws", "type TodoDto { description: String }")

        assertEquals(FileType, file.fileType)
        assertInstanceOf(file, File::class.java)
    }

    fun testParsesEveryDefinitionKind() {
        val file = myFixture.configureByText(
            "todo.ws",
            """
            |type TodoDto { description: String }
            |enum Status { OPEN, DONE }
            |endpoint GetTodos GET /todos -> { 200 -> TodoDto[] }
            |channel Todos -> TodoDto
            |rpc Ping {} -> String
            """.trimMargin(),
        )

        assertEquals(
            listOf(Parser.TypeDef, Parser.EnumDef, Parser.EndpointDef, Parser.ChannelDef, Parser.RpcDef),
            file.node.getChildren(null).map { it.elementType }.filterNot { it == Types.WHITE_SPACE },
        )
        assertEquals(
            listOf("TodoDto", "Status", "GetTodos", "Todos", "Ping"),
            PsiTreeUtil.findChildrenOfType(file, CustomTypeElementDef::class.java).map { it.name },
        )
        assertFalse(PsiTreeUtil.hasErrorElements(file))
    }

    fun testSeparatesTypeDefinitionsFromReferences() {
        val file = myFixture.configureByText(
            "todo.ws",
            """
            |type TodoDto { owner: User, tags: Tag[] }
            |endpoint GetTodos GET /todos -> { 200 -> TodoDto[] }
            """.trimMargin(),
        )

        assertEquals(
            listOf("TodoDto", "GetTodos"),
            PsiTreeUtil.findChildrenOfType(file, CustomTypeElementDef::class.java).map { it.name },
        )
        assertEquals(
            listOf("User", "Tag", "TodoDto"),
            PsiTreeUtil.findChildrenOfType(file, CustomTypeElementRef::class.java).map { it.name },
        )
        assertEquals("GetTodos", PsiTreeUtil.findChildOfType(file, EndpointDefElement::class.java)?.name)
    }
}

package community.flock.wirespec.ide.intellij

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.navigation.ChooseByNameContributor as PlatformChooseByNameContributor

class ChooseByNameContributorTest : BasePlatformTestCase() {

    fun testOffersTypeDefinitionsAsSymbols() {
        myFixture.addFileToProject(
            "todo.ws",
            "type TodoDto {\n    description: String\n}\nendpoint GetTodos GET /todos -> {\n    200 -> TodoDto[]\n}",
        )
        myFixture.addFileToProject("user.ws", "type User {\n    name: String\n}")

        val contributor = PlatformChooseByNameContributor.SYMBOL_EP_NAME.extensionList
            .filterIsInstance<ChooseByNameContributor>()
            .single()

        assertSameElements(contributor.getNames(project, false).toList(), "TodoDto", "User")
        val user = contributor.getItemsByName("User", "User", project, false).single() as PsiElement
        assertEquals("user.ws", user.containingFile.name)
    }
}

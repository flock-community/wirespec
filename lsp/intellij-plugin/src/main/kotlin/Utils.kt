package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement

object Utils {

    fun visitAllElements(element: PsiElement?): List<PsiElement> = element?.run {
        listOf(this) + children.flatMap(::visitAllElements)
    } ?: emptyList()

    fun getPresentation(element: PsiElement) = object : ItemPresentation {

        override fun getLocationString() = ""

        override fun getPresentableText() = element.text

        override fun getIcon(unused: Boolean) = Icons.FILE
    }

}

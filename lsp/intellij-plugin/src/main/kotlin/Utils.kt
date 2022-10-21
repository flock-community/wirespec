package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import javax.swing.Icon

object Utils {

    fun visitAllElements(element: PsiElement?): List<PsiElement> {
        return if (element != null) {
            listOf(element) + element.children
                .flatMap { visitAllElements(it) }
        } else {
            listOf()
        }

    }

    fun getPresentation(element: PsiElement): ItemPresentation {
        return object : ItemPresentation {

            override fun getLocationString(): String {
                return ""
            }

            override fun getPresentableText(): String {
                return element.text
            }

            override fun getIcon(unused: Boolean): Icon {
                return Icons.FILE
            }
        }
    }

}
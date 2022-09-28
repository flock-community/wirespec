package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import javax.swing.Icon

object Util {

    fun getPresentation(element: PsiElement): ItemPresentation {
        return object : ItemPresentation {

            override fun getLocationString(): String? {
                return element.containingFile.name
            }

            override fun getPresentableText(): String {
                return "adsfadsf"
            }

            override fun getIcon(unused: Boolean): Icon {
                return Icons.FILE
            }
        }
    }
}
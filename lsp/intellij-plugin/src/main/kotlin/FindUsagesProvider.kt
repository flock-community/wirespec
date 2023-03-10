package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.lang.findUsages.FindUsagesProvider as IntellijFindUsagesProvider


public class FindUsagesProvider : IntellijFindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            Lexer(),
            TokenSets.CUSTOM_TYPE,
            TokenSet.EMPTY,
            TokenSet.EMPTY
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is CustomTypeElement
    }

    override fun getHelpId(psiElement: PsiElement): String? {
        return null
    }

    override fun getType(element: PsiElement): String {
        if (element is CustomTypeElement) {
            return "custom Type"
        }
        return "";
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return if (element is CustomTypeElement) {
            element.name ?: ""
        } else "";
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return if (element is CustomTypeElement) {
            element.name ?: ""
        } else ""
    }
}

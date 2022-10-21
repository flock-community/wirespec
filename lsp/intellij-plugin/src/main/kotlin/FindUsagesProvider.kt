package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
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
        return psiElement is CustomTypeElementDef || psiElement is CustomTypeElementRef
    }

    override fun getHelpId(psiElement: PsiElement): String? {
        return null
    }

    override fun getType(element: PsiElement): String {
        if (element is CustomTypeElementDef) {
            return "property";
        }
        return "";
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return if (element is CustomTypeElementDef) {
            "getNodeText";
        } else "";
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return if (element is CustomTypeElementDef) {
            "getNodeText"
        } else ""
    }
}

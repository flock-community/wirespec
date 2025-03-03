package community.flock.wirespec.ide.intellij

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import community.flock.wirespec.ide.intellij.parser.CustomTypeElement
import com.intellij.lang.findUsages.FindUsagesProvider as IntellijFindUsagesProvider

class FindUsagesProvider : IntellijFindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        Lexer(),
        TokenSet.create(Types.TYPE_IDENTIFIER),
        TokenSet.EMPTY,
        TokenSet.EMPTY,
    )

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is CustomTypeElement

    override fun getHelpId(psiElement: PsiElement) = null

    override fun getType(element: PsiElement) = if (element is CustomTypeElement) "custom Type" else ""

    override fun getDescriptiveName(element: PsiElement) = (element as? CustomTypeElement)?.name ?: ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)
}

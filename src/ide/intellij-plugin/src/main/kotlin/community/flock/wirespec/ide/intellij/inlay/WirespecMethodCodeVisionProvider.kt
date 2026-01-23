package community.flock.wirespec.ide.intellij.inlay

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.FileType
import community.flock.wirespec.ide.intellij.parser.EndpointDefElement

abstract class WirespecMethodCodeVisionProvider : DaemonBoundCodeVisionProvider {

    override val groupId: String = "references"
    override val relativeOrderings: List<CodeVisionRelativeOrdering> =
        listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)
    override val defaultAnchor: CodeVisionAnchorKind = CodeVisionAnchorKind.Top

    protected fun createEntry(
        project: Project,
        text: String,
        endpointName: String,
        textOffset: Int,
        providerId: String
    ): Pair<TextRange, CodeVisionEntry> {
        val textRange = TextRange(textOffset, textOffset)
        return textRange to ClickableTextCodeVisionEntry(
            text = text,
            providerId = providerId,
            onClick = { _, _ ->
                findEndpoint(project, endpointName)?.let { endpoint ->
                    (endpoint.navigationElement as? Navigatable)?.navigate(true)
                }
            }
        )
    }

    private fun findEndpoint(project: Project, name: String): PsiElement? {
        val scope = GlobalSearchScope.allScope(project)
        return FileTypeIndex.getFiles(FileType, scope)
            .asSequence()
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? community.flock.wirespec.ide.intellij.File }
            .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, EndpointDefElement::class.java) }
            .firstOrNull { it.name == name }
    }
}

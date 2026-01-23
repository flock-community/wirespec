package community.flock.wirespec.ide.intellij.inlay

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.FileType
import community.flock.wirespec.ide.intellij.parser.EndpointDefElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KotlinMethodCodeVisionProvider : DaemonBoundCodeVisionProvider {
    override val name: String = "Kotlin Method Code Vision"
    override val id: String = "kotlin.method.code.vision"
    override val groupId: String = "references"

    override val relativeOrderings: List<CodeVisionRelativeOrdering> =
        listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)
    override val defaultAnchor: CodeVisionAnchorKind = CodeVisionAnchorKind.Top

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (file !is KtFile) return emptyList()

        return PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java)
            .mapNotNull { function ->
                function.toLightMethods().firstNotNullOfOrNull { method ->
                    method.findSuperMethods()
                        .mapNotNull { it.containingClass }
                        .firstNotNullOfOrNull { isWirespecHandler(it) }
                        ?.let { handlerInterface ->
                            val companion = handlerInterface.innerClasses.find { it.name == "Companion" }
                            val companionSource = companion?.navigationElement as? KtObjectDeclaration

                            val pathTemplate = companionSource?.getPropertyValue("pathTemplate")
                            val methodText = companionSource?.getPropertyValue("method")

                            if (pathTemplate != null || methodText != null) {
                                val text = listOfNotNull(methodText, pathTemplate).joinToString(" ")
                                val textRange = TextRange(function.textOffset, function.textOffset)
                                textRange to ClickableTextCodeVisionEntry(
                                    text = text,
                                    providerId = id,
                                    onClick = { _, _ ->
                                        handlerInterface.containingClass?.name?.let { endpointName ->
                                            findEndpoint(file.project, endpointName)?.let { endpoint ->
                                                (endpoint.navigationElement as? Navigatable)?.navigate(true)
                                            }
                                        }
                                    }
                                )
                            } else null
                        }
                }
            }
            .toList()
    }

    private fun findEndpoint(project: Project, name: String): PsiElement? {
        val scope = GlobalSearchScope.allScope(project)
        return FileTypeIndex.getFiles(FileType, scope)
            .asSequence()
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? community.flock.wirespec.ide.intellij.File }
            .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, EndpointDefElement::class.java) }
            .firstOrNull { it.name == name }
    }

    private fun KtObjectDeclaration.getPropertyValue(propertyName: String): String? {
        return declarations.filterIsInstance<KtProperty>()
            .firstOrNull { it.name == propertyName }
            ?.initializer
            ?.let { it as? KtStringTemplateExpression }
            ?.entries
            ?.joinToString("") { it.text }
    }

    private fun isWirespecHandler(psiClass: PsiClass): PsiClass? =
        if (InheritanceUtil.isInheritor(psiClass, "community.flock.wirespec.kotlin.Wirespec.Handler")) psiClass
        else null
}

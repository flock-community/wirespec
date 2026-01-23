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
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.FileType
import community.flock.wirespec.ide.intellij.parser.EndpointDefElement

class JavaMethodCodeVisionProvider : DaemonBoundCodeVisionProvider {
    override val name: String = "Java Method Code Vision"
    override val id: String = "java.method.code.vision"
    override val groupId: String = "references"

    override val relativeOrderings: List<CodeVisionRelativeOrdering> =
        listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)
    override val defaultAnchor: CodeVisionAnchorKind = CodeVisionAnchorKind.Top

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (file !is PsiJavaFile) return emptyList()
        return file.classes
            .flatMap { it.allMethods.toList() }
            .mapNotNull { method ->
                method.findSuperMethods()
                    .mapNotNull { it.containingClass }
                    .firstNotNullOfOrNull { isWirespecHandler(it) }
                    ?.let { handlerInterface ->
                        val handlers = handlerInterface.innerClasses.find { it.name == "Handlers" }

                        val pathTemplate = handlers?.getLiteralValue("getPathTemplate")
                        val methodText = handlers?.getLiteralValue("getMethod")

                        val text = listOfNotNull(methodText, pathTemplate).joinToString(" ")
                        val textRange = TextRange(method.textOffset, method.textOffset)
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
                    }
            }
    }

    private fun findEndpoint(project: Project, name: String): PsiElement? {
        val scope = GlobalSearchScope.allScope(project)
        return FileTypeIndex.getFiles(FileType, scope)
            .asSequence()
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? community.flock.wirespec.ide.intellij.File }
            .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, EndpointDefElement::class.java) }
            .firstOrNull { it.name == name }
    }

    private fun PsiClass.getLiteralValue(methodName: String): String? =
        findMethodsByName(methodName, false)
            .firstOrNull()
            ?.let { method ->
                PsiTreeUtil.findChildOfType(method, PsiReturnStatement::class.java)
                    ?.returnValue
                    ?.let { it as? PsiLiteralExpression }
                    ?.value as? String
            }

    private fun isWirespecHandler(psiClass: PsiClass): PsiClass? =
        if (InheritanceUtil.isInheritor(psiClass, "community.flock.wirespec.java.Wirespec.Handler")) psiClass
        else null
}

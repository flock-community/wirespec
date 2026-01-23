package community.flock.wirespec.ide.intellij.inlay

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KotlinMethodCodeVisionProvider : WirespecMethodCodeVisionProvider() {
    override val name: String = "Wirespec Kotlin Method Code Vision"
    override val id: String = "wirespec.kotlin.method.code.vision"

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

                                handlerInterface.containingClass?.name?.let { endpointName ->
                                    createEntry(file.project, text, endpointName, function.textOffset, id)
                                }
                            } else {
                                null
                            }
                        }
                }
            }
            .toList()
    }

    private fun KtObjectDeclaration.getPropertyValue(propertyName: String): String? = declarations.filterIsInstance<KtProperty>()
        .firstOrNull { it.name == propertyName }
        ?.initializer
        ?.let { it as? KtStringTemplateExpression }
        ?.entries
        ?.joinToString("") { it.text }

    private fun isWirespecHandler(psiClass: PsiClass): PsiClass? = if (InheritanceUtil.isInheritor(psiClass, "community.flock.wirespec.kotlin.Wirespec.Handler")) {
        psiClass
    } else {
        null
    }
}

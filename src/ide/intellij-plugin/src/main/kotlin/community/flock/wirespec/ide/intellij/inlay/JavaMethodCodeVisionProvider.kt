package community.flock.wirespec.ide.intellij.inlay

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil

class JavaMethodCodeVisionProvider : WirespecMethodCodeVisionProvider() {
    override val name: String = "Wirespec Java Method Code Vision"
    override val id: String = "wirespec.java.method.code.vision"

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

                        handlerInterface.containingClass?.name?.let { endpointName ->
                            createEntry(file.project, text, endpointName, method.textOffset, id)
                        }
                    }
            }
    }

    private fun PsiClass.getLiteralValue(methodName: String): String? = findMethodsByName(methodName, false)
        .firstOrNull()
        ?.let { method ->
            PsiTreeUtil.findChildOfType(method, PsiReturnStatement::class.java)
                ?.returnValue
                ?.let { it as? PsiLiteralExpression }
                ?.value as? String
        }

    private fun isWirespecHandler(psiClass: PsiClass): PsiClass? = if (InheritanceUtil.isInheritor(psiClass, "community.flock.wirespec.java.Wirespec.Handler")) {
        psiClass
    } else {
        null
    }
}

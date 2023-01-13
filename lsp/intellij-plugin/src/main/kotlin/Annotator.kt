package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger


class Annotator : ExternalAnnotator<List<CompilerException>, List<CompilerException>>() {

    private val logger = object : Logger(false) {}

    override fun collectInformation(file: PsiFile) = Wirespec.tokenize(file.text)
        .let { Parser(logger).parse(it) }
        .fold(
            ifLeft = { listOf(it) },
            ifRight = { listOf() }
        )

    override fun doAnnotate(collectedInfo: List<CompilerException>) = collectedInfo

    override fun apply(file: PsiFile, annotationResult: List<CompilerException>, holder: AnnotationHolder) {
        annotationResult.forEach {
            holder
                .newAnnotation(HighlightSeverity.ERROR, it.message ?: "")
                .range(
                    TextRange(
                        it.coordinates.idxAndLength.idx - it.coordinates.idxAndLength.length,
                        it.coordinates.idxAndLength.idx
                    )
                )
                .create()
        }
        super.apply(file, annotationResult, holder)
    }

}

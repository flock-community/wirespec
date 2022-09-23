package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.flatMap
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger


class Annotator : ExternalAnnotator<List<CompilerException>, List<CompilerException>>() {

    val logger = object : Logger(false) {}

    override fun collectInformation(file: PsiFile) = WireSpec.tokenize(file.text)
        .flatMap { Parser(logger).parse(it) }
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
package community.flock.wirespec.lsp.intellij_plugin

import arrow.core.flatMap
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger
import kotlinx.coroutines.runBlocking


class Annotator : ExternalAnnotator<List<WirespecException>, List<WirespecException>>() {

    private val logger = object : Logger() {}

    override fun collectInformation(file: PsiFile) = runBlocking {
        WirespecSpec.tokenize(file.text)
            .let { Parser(logger).parse(it) }
            .map { it.validate() }
            .fold({ it }, { emptyList() })
    }

    override fun doAnnotate(collectedInfo: List<WirespecException>?) = collectedInfo

    override fun apply(file: PsiFile, annotationResult: List<WirespecException>?, holder: AnnotationHolder) {
        annotationResult?.forEach {
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

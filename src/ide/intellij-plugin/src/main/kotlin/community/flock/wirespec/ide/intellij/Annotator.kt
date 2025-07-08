package community.flock.wirespec.ide.intellij

import arrow.core.nonEmptyListOf
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.NoLogger

class Annotator :
    ExternalAnnotator<List<WirespecException>, List<WirespecException>>(),
    NoLogger {

    override fun collectInformation(file: PsiFile) = WirespecSpec
        .tokenize(file.text)
        .let { parse(nonEmptyListOf(TokenizedModule("", it))) }
        .fold({ it }, { emptyList() })

    override fun doAnnotate(collectedInfo: List<WirespecException>?) = collectedInfo

    override fun apply(file: PsiFile, annotationResult: List<WirespecException>?, holder: AnnotationHolder) {
        annotationResult?.forEach {
            holder
                .newAnnotation(HighlightSeverity.ERROR, it.message)
                .range(
                    TextRange(
                        it.coordinates.idxAndLength.idx - it.coordinates.idxAndLength.length,
                        it.coordinates.idxAndLength.idx,
                    ),
                )
                .create()
        }
        super.apply(file, annotationResult, holder)
    }
}

package community.flock.wirespec.ide.intellij

import arrow.core.toNonEmptyListOrNull
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.NoLogger

class Annotator :
    ExternalAnnotator<Map<PsiFile, TokenizedModule>, List<WirespecException>>(),
    NoLogger {

    override fun collectInformation(file: PsiFile): Map<PsiFile, TokenizedModule> = file.containingDirectory.files
        .associateWith { file -> TokenizedModule(FileUri(file.name), WirespecSpec.tokenize(file.text)) }

    override fun doAnnotate(collectedInfo: Map<PsiFile, TokenizedModule>): List<WirespecException>? {
        println(collectedInfo.keys.map { it.name })
        return collectedInfo.values.toNonEmptyListOrNull()
            ?.let { parse(it) }
            ?.fold({ it }, { emptyList() })
    }

    override fun apply(
        file: PsiFile,
        annotationResult: List<WirespecException>?,
        holder: AnnotationHolder,
    ) {
        annotationResult
            ?.filter { it.fileUri.value == file.name }
            ?.forEach {
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

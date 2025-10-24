package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeCommentMarkers
import kotlin.jvm.JvmInline

sealed interface MetaData

interface HasMetaData :
    HasAnnotations,
    HasComment

interface HasAnnotations {
    val annotations: List<Annotation>
}

data class Annotation(
    val name: String,
    val parameters: List<Parameter>,
) : MetaData,
    Node {
    data class Parameter(
        val name: String,
        val value: Value,
    ) : Node
    sealed interface Value {
        data class Single(val value: String) : Value
        data class Array(val value: List<Single>) : Value
        data class Dict(val value: List<Parameter>) : Value
    }
}

interface HasComment {
    val comment: Comment?
}

@JvmInline
value class Comment private constructor(override val value: String) :
    MetaData,
    Value<String> {
    companion object {
        operator fun invoke(comment: String) = Comment(comment.removeCommentMarkers())
    }
}

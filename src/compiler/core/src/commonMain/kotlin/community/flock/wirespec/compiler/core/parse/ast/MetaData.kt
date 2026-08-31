package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeCommentMarkers
import kotlin.jvm.JvmInline

private sealed interface MetaData

public interface HasMetaData :
    HasAnnotations,
    HasComment

public interface HasAnnotations {
    public val annotations: List<Annotation>
}

public data class Annotation(
    val name: String,
    val parameters: List<Parameter>,
) : MetaData,
    Node {
    public data class Parameter(
        val name: String,
        val value: Value,
    ) : Node
    public sealed interface Value {
        public data class Single(val value: String) : Value
        public data class Array(val value: List<Single>) : Value
        public data class Dict(val value: List<Parameter>) : Value
    }
}

public interface HasComment {
    public val comment: Comment?
}

@JvmInline
public value class Comment private constructor(override val value: String) :
    MetaData,
    Value<String> {
    public companion object {
        public operator fun invoke(comment: String): Comment = Comment(comment.removeCommentMarkers())
    }
}

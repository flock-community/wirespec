package community.flock.wirespec.compiler.core.parse.ast

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.FileUri

public typealias AST = Root
public typealias Statements = NonEmptyList<Definition>

public sealed interface Node

/**
 * The specification: every module compiled together. Its [annotations] are the `@spec:` annotations of
 * all its files, in file order.
 */
public data class Root(
    val modules: NonEmptyList<Module>,
    val annotations: List<SpecAnnotation> = emptyList(),
) : Node

/** A file of the specification. Its [annotations] are the `@file:` annotations in its header. */
public data class Module(
    val fileUri: FileUri,
    val statements: Statements,
    val annotations: List<Annotation> = emptyList(),
) : Node

/** A `@spec:` annotation, with the file it is written in, so a conflict between two files can name both. */
public data class SpecAnnotation(
    val fileUri: FileUri,
    val annotation: Annotation,
) : Node

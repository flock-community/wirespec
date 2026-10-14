package community.flock.wirespec.compiler.test

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger

public fun compile(source: String): Compiler = { emitter: () -> Emitter ->
    object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter())
    }.compile(nonEmptyListOf(ModuleContent(FileUri("N/A"), source)))
        .map { emitted -> emitted.filter { !it.file.contains("Wirespec") } }
        .map { emitted -> emitted.joinToString("\n") { it.result } }
        .onLeft(::println)
}

/** Parses every source as its own module (`test0.ws`, `test1.ws`, ...) into one validated [AST]. */
public fun parse(source: String, vararg more: String): EitherNel<WirespecException, AST> = object : ParseContext, NoLogger {}
    .parse(nonEmptyListOf(source, *more).mapIndexed { index, content -> ModuleContent(FileUri("test$index.ws"), content) })

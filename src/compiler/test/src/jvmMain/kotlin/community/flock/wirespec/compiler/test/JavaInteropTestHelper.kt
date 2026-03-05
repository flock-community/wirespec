package community.flock.wirespec.compiler.test

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Root
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.NoLogger

object JavaInteropTestHelper {
    @JvmStatic
    fun parse(source: String): Root = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    @JvmStatic
    fun emit(emitter: Emitter, ast: Root, logger: Logger): List<Emitted> = emitter.emit(ast, logger)
}

package community.flock.wirespec.integration.spring.java.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.java.JavaEmitter

abstract class SpringJavaEmitterHelper(packageName: PackageName) : JavaEmitter(packageName, EmitShared(false)) {

    abstract fun injectFiles(definitions: List<Definition>): List<Emitted>

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> =
        super.emit(ast, logger).let { results ->
            injectFiles(ast.modules.flatMap { it.statements })
                .takeIf { it.isNotEmpty() }
                ?.let { results + it }
                ?: results
        }
}

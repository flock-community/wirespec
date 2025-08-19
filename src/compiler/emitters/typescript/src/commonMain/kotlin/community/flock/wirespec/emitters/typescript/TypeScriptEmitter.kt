package community.flock.wirespec.emitters.typescript

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.namespace
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class TypeScriptEmitter() : LanguageEmitter(), TypeScriptEnumDefinitionEmitter, TypeScriptIdentifierEmitter, TypeScriptTypeDefinitionEmitter, TypeScriptClientEmitter, TypeScriptEndpointDefinitionEmitter, TypeScriptRefinedTypeDefinitionEmitter {

    override val extension = FileExtension.TypeScript

    override val shared = TypeScriptShared

    override val singleLineComment = "//"

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super<LanguageEmitter>.emit(ast, logger)
        .run { if(ast.hasEndpoints()){ plus(emitClient(ast) ) } else this }
        .plus(
            ast.modules
                .flatMap { it.statements }
                .groupBy { def -> def.namespace() }
                .map { (ns, defs) ->
                    Emitted(
                        "${ns}/index.${extension.value}",
                        defs.joinToString("\n") { "export {${it.identifier.value}} from './${it.identifier.value}'" }
                    )
                }
        )

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = super<LanguageEmitter>.emit(module, logger).let {
        it + Emitted("Wirespec", shared.source)
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super<LanguageEmitter>.emit(definition, module, logger).let {
            val subPackageName = PackageName("") + definition
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = """
                    |import {Wirespec} from '../Wirespec'
                    |
                    |${it.result}
                """.trimMargin()
            )
        }

    override fun emit(union: Union) = """
        |${
        union.importReferences().distinctBy { it.value }.map { "import {${it.value}} from '../model'" }
            .joinToString("\n") { it.trimStart() }
    }
        |export type ${union.identifier.sanitizeSymbol()} = ${union.entries.joinToString(" | ") { it.emit() }}
        |
    """.trimMargin()


    override fun emit(channel: Channel) = notYetImplemented()
}

package community.flock.wirespec.emitters.java

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

interface E :
    JavaIdentifierEmitter,
    JavaTypeDefinitionEmitter,
    JavaEndpointDefinitionEmitter,
    JavaChannelDefinitionEmitter,
    JavaEnumDefinitionEmitter,
    JavaUnionDefinitionEmitter,
    JavaRefinedTypeDefinitionEmitter,
    JavaClientEmitter

open class JavaEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : E, Emitter() {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec;
        |
    """.trimMargin()

    override val extension = FileExtension.Java

    override val shared = JavaShared

    override val singleLineComment = "//"

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> {
        return super<Emitter>.emit(ast, logger)
            .run { if(ast.hasEndpoints()){ plus(emitClient(ast) ) } else this }
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super<Emitter>.emit(module, logger).let {
            if (emitShared.value) it + Emitted(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec", shared.source)
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super<Emitter>.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = """
                    |package $subPackageName;
                    |${if (module.needImports()) import else ""}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while",
            "true", "false"
        )
    }
}

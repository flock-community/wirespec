package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.AbstractEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.utils.Logger

interface E :
    KotlinIdentifierEmitter,
    KotlinTypeDefinitionEmitter,
    KotlinEndpointDefinitionEmitter,
    KotlinChannelDefinitionEmitter,
    KotlinEnumDefinitionEmitter,
    KotlinUnionDefinitionEmitter,
    KotlinRefinedTypeDefinitionEmitter

open class KotlinEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : AbstractEmitter(), E {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override val extension = FileExtension.Kotlin

    override val shared = KotlinShared

    override val singleLineComment = "//"

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super<AbstractEmitter>.emit(module, logger).let {
            if (emitShared.value) it + Emitted(
                PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec",
                shared.source
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super<AbstractEmitter>.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            Emitted(
                file = subPackageName.toDir() + it.file,
                result = """
                    |package $subPackageName
                    |${if (module.needImports()) import else ""}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while", "private", "public"
        )
    }
}

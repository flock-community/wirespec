package community.flock.wirespec.plugin

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.integration.avro.extension.AvroExtension
import community.flock.wirespec.integration.jackson.extension.JacksonExtension
import community.flock.wirespec.integration.jvm.extension.BuilderExtension
import community.flock.wirespec.integration.kotest.extension.KotestDslExtension
import community.flock.wirespec.integration.kotlinxserialization.extension.KotlinxSerializationExtension
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.integration.spring.extension.SpringNativeHintsExtension

public enum class Extension(private val languages: Set<FileExtension>) {
    Avro(setOf(FileExtension.Java, FileExtension.Kotlin)),
    Builder(setOf(FileExtension.Java, FileExtension.Kotlin, FileExtension.Scala)),
    Jackson(setOf(FileExtension.Java, FileExtension.Kotlin)),
    KotestDsl(setOf(FileExtension.Kotlin)),
    KotlinxSerialization(setOf(FileExtension.Kotlin)),
    SpringMappingAnnotations(setOf(FileExtension.Java, FileExtension.Kotlin)),
    SpringNativeHints(setOf(FileExtension.Java, FileExtension.Kotlin)),
    ;

    public fun supports(language: FileExtension): Boolean = language in languages

    public companion object {
        public fun toMap(): Map<String, Extension> = entries.associateBy { it.name }
        override fun toString(): String = entries.joinToString()
    }
}

private fun Extension.toIrExtension(packageName: PackageName, language: FileExtension): IrExtension = when (this) {
    Extension.Avro -> AvroExtension(packageName, language)
    Extension.Builder -> BuilderExtension(language)
    Extension.Jackson -> JacksonExtension()
    Extension.KotestDsl -> KotestDslExtension(packageName)
    Extension.KotlinxSerialization -> KotlinxSerializationExtension()
    Extension.SpringMappingAnnotations -> SpringMappingAnnotationsExtension(language)
    Extension.SpringNativeHints -> SpringNativeHintsExtension(packageName, language)
}

/** An extension leaves out the languages it does not support, so one list can serve every language of a run. */
public fun List<Extension>.toIrExtensions(packageName: PackageName, language: FileExtension): List<IrExtension> = this
    .filter { it.supports(language) }
    .map { it.toIrExtension(packageName, language) }

public fun Emitter.withExtensions(extensions: List<IrExtension>): Emitter = extensions.toNonEmptyListOrNull()
    ?.let { (this as? IrEmitter)?.applyExtensions(it) }
    ?: this

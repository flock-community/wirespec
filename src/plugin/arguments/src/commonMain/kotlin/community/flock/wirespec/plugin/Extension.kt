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
import kotlin.reflect.KClass

public enum class Extension(private val type: KClass<out IrExtension>, private val languages: Set<FileExtension>) {
    Avro(AvroExtension::class, setOf(FileExtension.Java, FileExtension.Kotlin)),
    Builder(BuilderExtension::class, setOf(FileExtension.Java, FileExtension.Kotlin, FileExtension.Scala)),
    Jackson(JacksonExtension::class, setOf(FileExtension.Java, FileExtension.Kotlin)),
    KotestDsl(KotestDslExtension::class, setOf(FileExtension.Kotlin)),
    KotlinxSerialization(KotlinxSerializationExtension::class, setOf(FileExtension.Kotlin)),
    SpringMappingAnnotations(SpringMappingAnnotationsExtension::class, setOf(FileExtension.Java, FileExtension.Kotlin)),
    SpringNativeHints(SpringNativeHintsExtension::class, setOf(FileExtension.Java, FileExtension.Kotlin)),
    ;

    public fun supports(language: FileExtension): Boolean = language in languages

    public companion object {
        public fun toMap(): Map<String, Extension> = entries.associateBy { it.name }

        /** The bundled extension implemented by [type], so an extension registered by class also skips the languages it does not support. */
        public fun of(type: KClass<*>): Extension? = entries.find { it.type == type }

        override fun toString(): String = entries.joinToString()
    }
}

/** Null for a language the extension does not support. */
public fun Extension.toIrExtension(packageName: PackageName, language: FileExtension): IrExtension? = takeIf { it.supports(language) }?.let {
    when (it) {
        Extension.Avro -> AvroExtension(packageName, language)
        Extension.Builder -> BuilderExtension(language)
        Extension.Jackson -> JacksonExtension()
        Extension.KotestDsl -> KotestDslExtension(packageName)
        Extension.KotlinxSerialization -> KotlinxSerializationExtension()
        Extension.SpringMappingAnnotations -> SpringMappingAnnotationsExtension(language)
        Extension.SpringNativeHints -> SpringNativeHintsExtension(packageName, language)
    }
}

public fun List<Extension>.toIrExtensions(packageName: PackageName, language: FileExtension): List<IrExtension> = mapNotNull { it.toIrExtension(packageName, language) }

public fun Emitter.withExtensions(extensions: List<IrExtension>): Emitter = extensions.toNonEmptyListOrNull()
    ?.let { (this as? IrEmitter)?.applyExtensions(it) }
    ?: this

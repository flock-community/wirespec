package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.integration.jackson.extension.JacksonExtension
import community.flock.wirespec.integration.kotest.extension.KotestDslExtension
import community.flock.wirespec.integration.kotlinxserialization.extension.KotlinxSerializationExtension
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.integration.spring.extension.SpringNativeHintsExtension
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.integration.avro.extension.AvroExtension as AvroIrExtension

public enum class Extension {
    Avro,
    Jackson,
    KotlinxSerialization,
    SpringMappingAnnotations,
    SpringNativeHints,
    KotestDsl,
    ;

    public companion object {
        public fun toMap(): Map<String, Extension> = entries.associateBy { it.name }
        override fun toString(): String = entries.joinToString()
    }
}

public fun Extension.toIrExtension(packageName: PackageName, language: FileExtension): IrExtension = when (this) {
    Extension.Avro -> AvroIrExtension(packageName, language)
    Extension.Jackson -> JacksonExtension()
    Extension.KotlinxSerialization -> KotlinxSerializationExtension()
    Extension.SpringMappingAnnotations -> SpringMappingAnnotationsExtension(language)
    Extension.SpringNativeHints -> SpringNativeHintsExtension(packageName, language)
    Extension.KotestDsl -> KotestDslExtension(packageName)
}

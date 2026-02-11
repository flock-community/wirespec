package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.converter.avro.AvroEmitter
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter

enum class Language {
    Java,
    Kotlin,
    TypeScript,
    Python,
    Wirespec,
    OpenAPIV2,
    OpenAPIV3,
    Avro,
    ;

    companion object {
        fun toMap() = entries.associateBy { it.name }
        override fun toString() = entries.joinToString()
    }
}

fun Language.toEmitter(packageName: PackageName, emitShared: EmitShared) = when (this) {
    Language.Java -> JavaEmitter(packageName, emitShared)
    Language.Kotlin -> KotlinEmitter(packageName, emitShared)
    Language.Python -> PythonEmitter(packageName, emitShared)
    Language.TypeScript -> TypeScriptEmitter()
    Language.Wirespec -> WirespecEmitter()
    Language.OpenAPIV2 -> OpenAPIV2Emitter
    Language.OpenAPIV3 -> OpenAPIV3Emitter
    Language.Avro -> AvroEmitter
}

fun Language.toIrEmitter(packageName: PackageName, emitShared: EmitShared) = when (this) {
    Language.Java -> JavaIrEmitter(packageName, emitShared)
    Language.Kotlin -> KotlinIrEmitter(packageName, emitShared)
    Language.Python -> PythonIrEmitter(packageName, emitShared)
    Language.TypeScript -> TypeScriptIrEmitter()
    Language.Wirespec -> WirespecEmitter()
    Language.OpenAPIV2 -> OpenAPIV2Emitter
    Language.OpenAPIV3 -> OpenAPIV3Emitter
    Language.Avro -> AvroEmitter
}

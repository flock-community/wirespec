package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
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
    Language.TypeScript -> TypeScriptEmitter(emitShared)
    Language.Wirespec -> WirespecEmitter()
    Language.OpenAPIV2 -> OpenAPIV2Emitter
    Language.OpenAPIV3 -> OpenAPIV3Emitter
}

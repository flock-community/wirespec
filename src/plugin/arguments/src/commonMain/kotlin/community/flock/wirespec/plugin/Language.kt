package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.converter.avro.AvroJsonEmitter
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.rust.RustEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter

public enum class Language {
    Java,
    Kotlin,
    TypeScript,
    Python,
    Rust,
    Scala,
    Wirespec,
    OpenAPIV2,
    OpenAPIV3,
    Avro,
    ;

    public companion object {
        public fun toMap(): Map<String, Language> = entries.associateBy { it.name }
        override fun toString(): String = entries.joinToString()
    }
}

/** [openApiTitle] and [openApiVersion] win over the title and version in `@spec:OpenAPIInfo` of an OpenAPI document. */
public fun Language.toEmitter(
    packageName: PackageName,
    emitShared: EmitShared,
    openApiTitle: String? = null,
    openApiVersion: String? = null,
): Emitter = when (this) {
    Language.Java -> JavaEmitter(packageName, emitShared)
    Language.Kotlin -> KotlinEmitter(packageName, emitShared)
    Language.Python -> PythonEmitter(packageName, emitShared)
    Language.Rust -> RustEmitter(packageName, emitShared)
    Language.Scala -> ScalaEmitter(packageName, emitShared)
    Language.TypeScript -> TypeScriptEmitter()
    Language.Wirespec -> WirespecEmitter()
    Language.OpenAPIV2 -> OpenAPIV2Emitter.withOptions(OpenAPIV2Emitter.Options(openApiTitle, openApiVersion))
    Language.OpenAPIV3 -> OpenAPIV3Emitter.withOptions(OpenAPIV3Emitter.Options(openApiTitle, openApiVersion))
    Language.Avro -> AvroJsonEmitter
}

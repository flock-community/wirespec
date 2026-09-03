package community.flock.wirespec.plugin

import community.flock.wirespec.emitters.rust.RustSerdeExtension
import community.flock.wirespec.ir.extension.IrExtension

/**
 * Built-in [IrExtension]s addressable by name. The CLI cannot load extension
 * classes reflectively like the Maven plugin (it also ships as a native
 * binary), so it selects from this registry instead.
 */
public val extensionsByName: Map<String, IrExtension> = mapOf(
    "RustSerde" to RustSerdeExtension(),
)

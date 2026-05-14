import org.gradle.api.provider.Property

abstract class EmitterFixturesExtension {

    abstract val language: Property<String>

    abstract val emitterPackage: Property<String>

    /** Simple name of the emitter class, e.g. `JavaIrEmitter`. */
    abstract val emitterClass: Property<String>

    /** Fully-qualified name of the generator class used by `sharedOutputTest`. */
    abstract val generatorClass: Property<String>

    /** Include the four `testEmitter*` fixtures driven by NodeFixtures. */
    abstract val includesTestEmitterFixtures: Property<Boolean>

    /** Emitter constructor takes `emitShared = EmitShared(true)`. False for TypeScript. */
    abstract val emitterAcceptsEmitShared: Property<Boolean>

    /**
     * How `sharedOutputTest` extracts text from the emitter:
     *   - "generator": `emitShared()?.let(<Generator>::generate)`
     *   - "elements":  `emitShared()?.elements?.filterIsInstance<RawElement>()?.joinToString("") { it.code }`
     */
    abstract val sharedOutputStyle: Property<String>
}

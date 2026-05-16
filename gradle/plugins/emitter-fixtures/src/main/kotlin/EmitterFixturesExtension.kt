import org.gradle.api.provider.Property

interface EmitterFixturesExtension {

    /** Kotlin package that owns the per-emitter test code. Used to place the
     *  generated `EmitterFixtures.kt`. */
    val emitterPackage: Property<String>

    /** Fully-qualified class name of the IrEmitter to drive `updateEmitterFixtures`. */
    val emitterClass: Property<String>
}

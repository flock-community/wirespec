import org.gradle.api.provider.Property

interface EmitterFixturesExtension {

    /** Fully-qualified class name of the IrEmitter to drive `updateEmitterFixtures`.
     *  Its package portion also locates the generated `EmitterFixtures.kt`. */
    val emitterClass: Property<String>
}

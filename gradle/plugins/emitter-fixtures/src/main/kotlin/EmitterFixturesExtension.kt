import org.gradle.api.provider.Property

abstract class EmitterFixturesExtension {

    /** Kotlin package that owns the per-emitter test code. Used both to place the
     *  generated `EmitterFixtures.kt` and to derive the `UpdateEmitterFixtures` main. */
    abstract val emitterPackage: Property<String>
}

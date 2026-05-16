import org.gradle.api.provider.Property

interface EmitterFixturesExtension {

    /** Kotlin package that owns the per-emitter test code. Used both to place the
     *  generated `EmitterFixtures.kt` and to derive the `UpdateEmitterFixtures` main. */
    val emitterPackage: Property<String>
}

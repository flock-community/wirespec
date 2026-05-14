import org.gradle.api.provider.Property

abstract class EmitterFixturesExtension {
    abstract val language: Property<String>
    abstract val updaterMainClass: Property<String>
    abstract val emitterPackage: Property<String>
}

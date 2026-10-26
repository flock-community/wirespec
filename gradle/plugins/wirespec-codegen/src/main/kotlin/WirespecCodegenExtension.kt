import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface WirespecCodegenExtension {

    /** Each language is emitted into `<basePackage>.<language>.generated`. */
    val basePackage: Property<String>

    /** Directory whose `.ws` files are compiled. Defaults to `src/jvmTest/resources/wirespec`. */
    val sources: DirectoryProperty

    /** Files in [sources] to convert, mapped to their CLI format, e.g. `"petstore.json" to "OpenAPIV3"`. */
    val conversions: MapProperty<String, String>

    /** CLI languages, e.g. `Java`, `Kotlin`, `Scala`. */
    val languages: ListProperty<String>

    /** Bundled IR extensions by CLI name, applied to the languages they support. */
    val irExtensions: ListProperty<String>

    /** Directory the sources are generated into, one subdirectory per language. */
    val outputDir: DirectoryProperty
}

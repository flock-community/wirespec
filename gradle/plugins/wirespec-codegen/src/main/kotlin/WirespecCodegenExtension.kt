import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface WirespecCodegenExtension {

    /** Package of the module; the codegen Main is expected at `<basePackage>.codegen.MainKt`. */
    val basePackage: Property<String>

    /** Directory the codegen Main reads from. Defaults to `src/jvmCodegen/resources/wirespec`. */
    val sources: DirectoryProperty

    /** Directory the codegen Main writes to, one subdirectory per language. */
    val outputDir: DirectoryProperty
}

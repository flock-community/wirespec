package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.plugin.Language.Wirespec
import java.io.File

fun Emitted.writeToFiles(
    output: File,
    packageName: PackageName?,
    shared: Shared?,
    fileName: String? = null,
    ext: FileExtension
) {
    if (shared != null) {
        val sharedPackageName = PackageName("community.flock.wirespec")
        writeFile(output, sharedPackageName, Wirespec.name, ext).writeText(shared.source)
    }
    writeFile(output, packageName, fileName ?: typeName, ext).writeText(result)

}

private fun writeFile(output: File, packageName: PackageName?, fileName: String, ext: FileExtension) =
    output
        .resolve(packageName.toDirectory())
        .apply { mkdirs()}
        .resolve("$fileName.${ext.value}")

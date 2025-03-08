package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.plugin.Language.Wirespec
import java.io.File

fun List<Emitted>.writeToFiles(
    output: File,
    packageName: PackageName?,
    shared: Shared?,
    fileName: String? = null,
    ext: FileExtension,
) {
    if (isEmpty()) return

    if (shared != null) {
        val sharedPackageName = PackageName(shared.packageString)
        writeFile(output, sharedPackageName, Wirespec.name, ext).writeText(shared.source)
    }

    forEach {
        writeFile(
            output = output,
            packageName = packageName,
            fileName = fileName ?: it.typeName,
            ext = ext,
        ).writeText(it.result)
    }
}

private fun writeFile(output: File, packageName: PackageName?, fileName: String, ext: FileExtension) = output
    .resolve(packageName.toDirectory())
    .apply { mkdirs() }
    .resolve("$fileName.${ext.value}")

package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.plugin.Language.Wirespec
import java.io.File as JavaFile

fun List<Emitted>.writeToFiles(
    output: JavaFile,
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
            fileName = fileName ?: it.file,
            ext = ext,
        ).writeText(it.result)
    }
}

private fun writeFile(output: JavaFile, packageName: PackageName?, fileName: String, ext: FileExtension) = output
    .resolve(packageName.toDirectory())
    .apply { mkdirs() }
    .resolve("$fileName.${ext.value}")

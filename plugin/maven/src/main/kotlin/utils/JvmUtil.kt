package community.flock.wirespec.plugin.maven.utils

import java.io.File

object JvmUtil {

    fun emitJvm(packageName: String, targetDirectory: String, name: String, ext: String) =
        renderPackagePath(packageName)
            .let { "$targetDirectory/$it" }
            .also { File(it).mkdirs() }
            .let { File("$it/$name.$ext") }

    private fun renderPackagePath(packageName: String) =
        if (packageName.isBlank()) "" else packageName.split('.').joinToString("/")

}
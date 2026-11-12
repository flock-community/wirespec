package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.plugin.io.Source.Type.Wirespec
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

private val wirespecExtension = ".${FileExtension.Wirespec.value}"

/**
 * Reads the file at this path from the first [classpath] entry, a directory or a jar, that holds it.
 */
public fun <E : Source.Type> ClassPath.readSource(classpath: Collection<File>): Source<E>? = value.trim('/').let { path ->
    classpath.absoluteEntries()
        .firstNotNullOfOrNull { it.readEntry(path) }
        ?.let { Source(Name(path.substringAfterLast('/').substringBefore('.')), it) }
}

/**
 * Reads the Wirespec file at this path or, when the path does not end with `.ws`, every Wirespec file beneath this
 * folder across all [classpath] entries. A folder without a slash is also tried as a package name: `com.example.api`.
 */
public fun ClassPath.wirespecSources(classpath: Collection<File>): List<Source<Wirespec>> = when {
    value.endsWith(wirespecExtension) -> listOfNotNull(readSource<Wirespec>(classpath))
    else -> value.trim('/').let { folder ->
        classpath.absoluteEntries().let { entries ->
            entries.wirespecSourcesIn(folder)
                .takeIf { it.isNotEmpty() || '/' in folder }
                ?: entries.wirespecSourcesIn(folder.replace('.', '/'))
        }
    }
}

private fun Collection<File>.absoluteEntries() = map { it.absoluteFile }.distinctBy { it.path }

private fun List<File>.wirespecSourcesIn(folder: String) = flatMap { entry ->
    when {
        entry.isDirectory -> entry.resolve(folder)
            .takeIf { it.isDirectory }
            ?.walkTopDown()
            ?.filter { it.isFile && it.name.endsWith(wirespecExtension) }
            ?.map { Source<Wirespec>(Name(it.name.removeSuffix(wirespecExtension)), it.readText(Charsets.UTF_8)) }
            ?.toList()
            .orEmpty()

        entry.isJar() -> JarFile(entry).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("$folder/") && it.name.endsWith(wirespecExtension) }
                .map { Source<Wirespec>(Name(it.name.substringAfterLast('/').removeSuffix(wirespecExtension)), jar.read(it)) }
                .toList()
        }

        else -> emptyList()
    }
}.distinct()

private fun File.readEntry(path: String) = when {
    isDirectory -> resolve(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)
    isJar() -> JarFile(this).use { jar -> jar.getJarEntry(path)?.takeUnless { it.isDirectory }?.let(jar::read) }
    else -> null
}

private fun JarFile.read(entry: JarEntry) = getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }

private fun File.isJar() = isFile && extension.equals("jar", ignoreCase = true)

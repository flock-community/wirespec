package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file

/**
 * Builds one top-level catalog `object` per source controller (`.ws` module),
 * named after the controller. `PetControllerV1.createPet…` reads as a bare
 * object access — no `scenario { }` receiver — and IDE completion stays scoped
 * to that controller's operations.
 *
 * The generated `*Call` classes live in the same `<pkg>.kotest` package, so the
 * object references them without imports and reaches their `internal`
 * constructors within the consumer's generated module.
 */
internal object CatalogFile {

    fun build(
        catalogName: String,
        endpointNames: List<String>,
        channelNames: List<String>,
        packageName: PackageName,
    ): File {
        val kotestPkg = "${packageName.value}.kotest"
        val fileName = PackageName(kotestPkg).toDir() + "${catalogName}Catalog"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)
            raw(renderCatalogObject(catalogName, endpointNames, channelNames))
        }
    }

    private fun renderCatalogObject(
        catalogName: String,
        endpointNames: List<String>,
        channelNames: List<String>,
    ): String = buildString {
        appendLine("public object $catalogName {")
        // Endpoints are block-style: `runs.putTodo { … }` opens a `<Endpoint>Scope` receiver.
        endpointNames.forEach { name ->
            val fn = name.replaceFirstChar(Char::lowercaseChar)
            appendLine("    public suspend fun <R> $fn(block: suspend ${name}Scope.() -> R): R =")
            appendLine("        ${name}Scope().block()")
        }
        // Channels keep the fluent `<Channel>Call` accessor.
        channelNames.forEach { name ->
            val dslName = name.replaceFirstChar(Char::lowercaseChar)
            appendLine("    public val $dslName: ${name}Call")
            appendLine("        get() = ${name}Call()")
        }
        append("}")
    }
}

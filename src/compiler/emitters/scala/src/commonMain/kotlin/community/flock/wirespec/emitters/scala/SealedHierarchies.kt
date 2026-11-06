package community.flock.wirespec.emitters.scala

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Enum
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Main
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Union
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.parse.ast.AST

/**
 * Scala only lets a type extend a `sealed trait` in the file that declares it, so every union shares a file with its
 * members: the file of the union takes in the files of its members, with their imports and companions. A type in two
 * unions, or a union in a union, brings all of them into one file. The members stay in their package, so references
 * to them do not change.
 *
 * The Scala emitter applies it right after the [community.flock.wirespec.compiler.core.ir.extension.ValidateExtension],
 * so the extensions registered on it see a union and its members in one file.
 */
internal object SealedHierarchies : IrExtension {
    override fun extend(ir: IR, ast: AST): IR = ir.partition { it is File }
        .let { (files, rest) -> files.filterIsInstance<File>().withSealedHierarchiesTogether() + rest }
        .toNonEmptyListOrNull()
        ?: ir
}

private fun List<File>.withSealedHierarchiesTogether(): List<File> {
    val declarations: Map<Pair<String?, String>, Int> = withIndex()
        .flatMap { (index, file) -> file.declaredNames().map { (file.packagePath() to it) to index } }
        .distinctBy { it.first }
        .toMap()

    val hierarchies: List<Set<Int>> = withIndex()
        .flatMap { (index, file) ->
            file.elements.filterIsInstance<Union>()
                .flatMap { it.members }
                .mapNotNull { member -> declarations[file.packagePath() to member.name.pascalCase()] }
                .filter { it != index }
                .map { index to it }
        }
        .fold(emptyList<Set<Int>>()) { groups, (union, member) ->
            groups.partition { union in it || member in it }
                .let { (touching, rest) -> rest + listOf(touching.flatten().toSet() + union + member) }
        }

    val hierarchyOf: Map<Int, Set<Int>> = hierarchies.flatMap { hierarchy -> hierarchy.map { it to hierarchy } }.toMap()

    return indices.mapNotNull { index ->
        when (val hierarchy = hierarchyOf[index]) {
            null -> this[index]
            else -> index.takeIf { it == anchorOf(hierarchy) }?.let { merge(hierarchy) }
        }
    }
}

/** The file a hierarchy is written to: the first of its files that declares a union. */
private fun List<File>.anchorOf(hierarchy: Set<Int>): Int = hierarchy.filter { index -> this[index].elements.any { it is Union } }.min()

private fun List<File>.merge(hierarchy: Set<Int>): File = anchorOf(hierarchy)
    .let { anchor -> (listOf(anchor) + hierarchy.sorted().filter { it != anchor }).map { this[it] } }
    .let { files ->
        files.first().copy(
            elements = files.first().elements.filterIsInstance<Package>() +
                files.flatMap { it.elements.filterIsInstance<Import>() }.distinct() +
                files.flatMap { file -> file.elements.filterNot { it is Package || it is Import } },
        )
    }

private fun File.packagePath(): String? = elements.filterIsInstance<Package>().firstOrNull()?.path

private fun File.declaredNames(): List<String> = elements.mapNotNull { it.declaredName() }

/** A type another type can extend: a namespace is a companion, and the rest declare no type. */
private fun Element.declaredName(): String? = when (this) {
    is Struct -> name.pascalCase()
    is Union -> name.pascalCase()
    is Enum -> name.pascalCase()
    is Interface -> name.pascalCase()
    is File, is Package, is Import, is Field, is Function, is Namespace, is Main, is RawElement -> null
}

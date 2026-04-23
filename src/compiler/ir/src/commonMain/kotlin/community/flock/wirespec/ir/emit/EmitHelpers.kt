package community.flock.wirespec.ir.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.namespace
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package

fun File.placeInPackage(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return File(
        name = Name.of(subPackageName.toDir() + name.pascalCase()),
        elements = listOf(Package(subPackageName.value)) + elements,
    )
}

fun File.placeInPackage(
    packageName: PackageName,
    definition: Definition,
): File = placeInPackage(packageName, definition.namespace())

fun File.prependImports(imports: List<Element>?): File = if (imports == null) {
    this
} else {
    copy(elements = imports + elements)
}

fun File.placeInModule(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return copy(name = Name.of(subPackageName.toDir() + name.pascalCase()))
}

fun File.placeInModule(
    packageName: PackageName,
    definition: Definition,
): File = placeInModule(packageName, definition.namespace())

fun NonEmptyList<File>.withSharedSource(
    emitShared: EmitShared,
    sharedFile: () -> File,
): NonEmptyList<File> = if (emitShared.value) this + sharedFile() else this

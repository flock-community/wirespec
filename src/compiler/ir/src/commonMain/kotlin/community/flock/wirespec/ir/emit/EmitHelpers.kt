package community.flock.wirespec.ir.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package

fun File.wrapWithPackage(
    packageName: PackageName,
    definition: Definition,
    wirespecImport: Element,
    needsImport: Boolean,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File {
    val subPackageName = packageName + definition
    return File(
        name = Name.of(subPackageName.toDir() + nameTransform(name)),
        elements = buildList {
            add(Package(subPackageName.value))
            if (needsImport) add(wirespecImport)
            addAll(elements)
        }
    )
}

fun File.wrapWithModuleImport(
    packageName: PackageName,
    definition: Definition,
    imports: List<Element>,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File {
    val subPackageName = packageName + definition
    return File(
        name = Name.of(subPackageName.toDir() + nameTransform(name)),
        elements = imports + elements,
    )
}

fun NonEmptyList<File>.withSharedSource(
    emitShared: EmitShared,
    sharedFile: () -> File,
): NonEmptyList<File> = if (emitShared.value) this + sharedFile() else this

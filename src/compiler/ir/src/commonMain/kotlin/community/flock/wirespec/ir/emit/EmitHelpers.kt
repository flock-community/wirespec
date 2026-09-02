package community.flock.wirespec.ir.emit

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.namespace
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package

public fun File.placeInPackage(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return File(
        name = Name.of(subPackageName.toDir() + name.pascalCase()),
        elements = listOf(Package(subPackageName.value)) + elements,
    )
}

public fun File.placeInPackage(
    packageName: PackageName,
    definition: Definition,
): File = placeInPackage(packageName, definition.namespace())

public fun File.prependImports(imports: List<Element>?): File = if (imports == null) {
    this
} else {
    copy(elements = imports + elements)
}

public fun File.placeInModule(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return copy(name = Name.of(subPackageName.toDir() + name.pascalCase()))
}

public fun File.placeInModule(
    packageName: PackageName,
    definition: Definition,
): File = placeInModule(packageName, definition.namespace())

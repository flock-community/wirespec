package community.flock.wirespec.example.maven.custom.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.extension.IrExtension

/**
 * An IR extension that appends a minimal `<Definition>Custom` class for every Wirespec definition,
 * next to the files the built-in emitter produced. It receives the complete language-neutral IR
 * together with the parsed AST, so it can add, drop, or reshape any element before the generator
 * turns the tree into source. The Wirespec plugin injects the configured [PackageName].
 */
class CustomExtension(
    packageName: PackageName,
) : IrExtension {
    private val customPackage = PackageName("$packageName.custom")

    override fun extend(
        ir: IR,
        ast: AST,
    ): IR = ir + customFiles(ast)

    private fun customFiles(ast: AST) =
        ast.modules
            .flatMap { it.statements }
            .map { "${it.identifier.value}Custom" }
            .map { className ->
                File(
                    name = Name.of(customPackage.toDir() + className),
                    elements = listOf(Package(customPackage.value), RawElement("public class $className {}")),
                )
            }
}

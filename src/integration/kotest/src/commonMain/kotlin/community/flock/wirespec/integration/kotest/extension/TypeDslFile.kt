package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file

/**
 * Builds the per-type Kotest DSL file (`<Type>Dsl.kt`): the reusable `<Type>Builder` (see
 * [RecordBuilder]) plus a `TodoDto.generate { … }: Gen<TodoDto>` entry point mirroring the
 * endpoint/channel `<X>.generate`. `generate` is an extension on the type's companion object
 * ([KotestDslExtension] injects one into each record); a field-less record has no companion,
 * so its `generate` extends the emitted `object` itself instead.
 */
internal object TypeDslFile {

    fun build(
        type: Type,
        packageName: PackageName,
        types: Map<String, Type> = emptyMap(),
        refined: Map<String, Refined> = emptyMap(),
    ): File {
        val shape = TypeShape.from(type, types, refined)
        val kotestPkg = "${packageName.value}.kotest"
        val modelPkg = "${packageName.value}.model"
        val fileName = PackageName(kotestPkg).toDir() + "${shape.name}Dsl"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("community.flock.wirespec.integration.kotest.dsl", "recordGen")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            import("io.kotest.property", "Gen")
            // pascalCase so underscore-bearing type names resolve to the emitted class.
            (listOf(shape.name) + shape.modelImports).distinct().forEach { import(modelPkg, Name.of(it).pascalCase()) }

            raw(renderEntryPoint(shape))
            raw(RecordBuilder.renderBuilderClass(shape.name, shape.fields))
        }
    }

    private fun renderEntryPoint(shape: TypeShape): String = buildString {
        val model = Name.of(shape.name).pascalCase()
        val builder = RecordBuilder.builderName(shape.name)
        // Field-less records emit as `object`s with no companion, so `generate` extends the object itself.
        val receiver = if (shape.fields.isEmpty()) model else "$model.Companion"
        appendLine("public fun $receiver.generate(block: $builder.() -> Unit = {}): Gen<$model> {")
        appendLine("    val builder = $builder().apply(block)")
        appendLine("    return recordGen<$model> {")
        append(RecordBuilder.renderRegistration(shape.fields, "builder", emptyList(), "        "))
        appendLine("    }")
        append("}")
    }
}

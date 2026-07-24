package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.Type as IrType

/** Builds the per-type Kotest DSL file (`<Type>Dsl.kt`): the reusable `<Type>Builder` plus a `generate` entry point. */
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

        val model = Name.of(shape.name).pascalCase()
        val builder = RecordBuilder.builderName(shape.name)
        val receiver = if (shape.fields.isEmpty()) model else "$model.Companion"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("community.flock.wirespec.integration.kotest.dsl", "recordGen")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            import("io.kotest.property", "Gen")
            if (shape.fields.isNotEmpty()) {
                import("io.kotest.property", "Arb")
                import("io.kotest.property.arbitrary", "constant")
            }
            (listOf(shape.name) + shape.modelImports).distinct().forEach { import(modelPkg, Name.of(it).pascalCase()) }

            function("generate") {
                visibility(Visibility.PUBLIC)
                receiver(IrType.Custom(receiver))
                arg("block", functionType(returnType = IrType.Unit, receiver = IrType.Custom(builder)), rawExpr("{}"))
                returnType(IrType.Custom("Gen", listOf(IrType.Custom(model))))
                raw("val builder = $builder().apply(block)")
                raw("return recordGen<$model> {")
                raw(RecordBuilder.renderRegistration(shape.fields, "builder", emptyList(), "    ").trimEnd())
                raw("}")
            }

            elements.add(RecordBuilder.buildBuilderClass(shape.name, shape.fields))
        }
    }
}

/** The Kotest-DSL view of a standalone record [Type]: its name, field shapes, and model imports. */
internal data class TypeShape(
    val name: String,
    val fields: List<EndpointShape.BodyFieldShape>,
    val modelImports: List<String>,
) {
    companion object {
        fun from(
            type: Type,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): TypeShape {
            val name = type.identifier.value
            val fields = EndpointShape.extractBodyFields(name, types, refined, visited = emptySet())
            val directRefs = type.shape.value.map { it.reference }
            val modelImports = EndpointShape.modelImportsFor(directRefs, fields, types)
            return TypeShape(name = name, fields = fields, modelImports = modelImports)
        }
    }
}

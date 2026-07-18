package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.extension.EndpointShape.BodyFieldShape
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.core.Type as IrType

/**
 * Builds the single reusable per-record builder class `<Type>Builder` (emitted once in the
 * type's own `<Type>Dsl.kt`) as an IR [Struct], and renders the `registerPath` lines that
 * apply it. The type, endpoint-body and channel-payload DSLs reference it by name instead of
 * emitting their own.
 */
internal object RecordBuilder {

    fun builderName(typeName: String): String = "${typeName}Builder"

    /** Nested fields reference the nested type's own `<Nested>Builder`; this builder never emits it. */
    fun buildBuilderClass(typeName: String, fields: List<BodyFieldShape>): Struct = struct(builderName(typeName)) {
        plainClass()
        annotation("@WirespecScenarioDsl")
        visibility(Visibility.PUBLIC)
        fields.forEach { f ->
            when (f) {
                is BodyFieldShape.Primitive ->
                    property(
                        name = f.name,
                        type = genNullable(IrType.Custom(f.kotlinType)),
                        isMutable = true,
                        visibility = Visibility.PUBLIC,
                        initializer = rawExpr("null"),
                    )
                is BodyFieldShape.NestedObject -> nestedBlock(f.name, f.typeName, genNullable(IrType.Custom(f.typeName)))
                is BodyFieldShape.NestedList ->
                    nestedBlock(f.name, f.elementTypeName, genNullable(IrType.Array(IrType.Custom(f.elementTypeName))))
            }
        }
    }

    /** `Gen<[element]>?`, the type of a builder override slot. */
    private fun genNullable(element: IrType): IrType = IrType.Nullable(IrType.Custom("Gen", listOf(element)))

    /** `<nested>.() -> Unit`, the sub-builder block type. */
    private fun blockType(nested: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(nested))

    /**
     * Emits the three members for a nested object/list field: the whole-value `Gen<…>?` override,
     * the `@PublishedApi internal var _<field>Block` sub-builder slot, and the `<field>Block { … }`
     * function that assigns it.
     */
    private fun community.flock.wirespec.ir.core.StructBuilder.nestedBlock(fieldName: String, nestedTypeName: String, genType: IrType) {
        val nested = builderName(nestedTypeName)
        property(
            name = fieldName,
            type = genType,
            isMutable = true,
            visibility = Visibility.PUBLIC,
            initializer = rawExpr("null"),
        )
        property(
            name = "_${fieldName}Block",
            type = IrType.Nullable(blockType(nested)),
            isMutable = true,
            visibility = Visibility.INTERNAL,
            annotations = listOf("@PublishedApi"),
            initializer = rawExpr("null"),
        )
        function("${fieldName}Block") {
            visibility(Visibility.PUBLIC)
            arg("block", blockType(nested))
            raw("${KotlinIdentifier.escape("_${fieldName}Block")} = block")
        }
    }

    /**
     * `registerPath(...)` lines applying a builder's override `Gen`s onto a
     * `KotestWirespecGeneratorBuilder` receiver, drilling into nested `<field>Block` sub-builders.
     * [path] is the wire-name segments so far (a nested list adds a `"*"` wildcard segment).
     */
    fun renderRegistration(
        fields: List<BodyFieldShape>,
        receiver: String,
        path: List<String>,
        indent: String,
    ): String = buildString {
        fields.forEach { f ->
            val fieldRef = "$receiver.${KotlinIdentifier.escape(f.name)}"
            val blockRef = "$receiver.${KotlinIdentifier.escape("_${f.name}Block")}"
            val segs = (path + f.name).joinToString(", ") { "\"$it\"" }
            when (f) {
                is BodyFieldShape.Primitive ->
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                is BodyFieldShape.NestedObject -> {
                    val nestedVar = KotlinIdentifier.escape("nested_${f.name}")
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                    appendLine("$indent$blockRef?.let { block ->")
                    appendLine("$indent    val $nestedVar = ${builderName(f.typeName)}().apply(block)")
                    append(renderRegistration(f.fields, nestedVar, path + f.name, "$indent    "))
                    appendLine("$indent}")
                }
                is BodyFieldShape.NestedList -> {
                    val nestedVar = KotlinIdentifier.escape("nested_${f.name}")
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                    appendLine("$indent$blockRef?.let { block ->")
                    appendLine("$indent    val $nestedVar = ${builderName(f.elementTypeName)}().apply(block)")
                    append(renderRegistration(f.fields, nestedVar, path + f.name + "*", "$indent    "))
                    appendLine("$indent}")
                }
            }
        }
    }
}

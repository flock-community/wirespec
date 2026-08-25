package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.integration.kotest.convert.EndpointShape.BodyFieldShape
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.StructBuilder
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.ir.core.Type as IrType

internal object RecordBuilder {

    fun builderName(typeName: String): String = "${Name.of(typeName).pascalCase()}Builder"

    fun buildBuilderClass(typeName: String, fields: List<BodyFieldShape>): Struct = struct(builderName(typeName)) {
        plainClass()
        annotation("@WirespecScenarioDsl")
        visibility(Visibility.PUBLIC)
        fields.forEach { f ->
            when (f) {
                is BodyFieldShape.Primitive -> {
                    property(
                        name = f.name,
                        type = genNullable(f.type),
                        isMutable = true,
                        visibility = Visibility.PUBLIC,
                        initializer = rawExpr("null"),
                    )
                    valueSetter(f.name, f.type)
                }
                is BodyFieldShape.NestedObject ->
                    nestedBlock(f.name, f.typeName, genNullable(IrType.Custom(f.typeName)), IrType.Custom(f.typeName))
                is BodyFieldShape.NestedList ->
                    nestedBlock(
                        f.name,
                        f.elementTypeName,
                        genNullable(IrType.Array(IrType.Custom(f.elementTypeName))),
                        IrType.Array(IrType.Custom(f.elementTypeName)),
                    )
            }
        }
    }

    private fun genNullable(element: IrType): IrType = IrType.Nullable(IrType.Custom("Gen", listOf(element)))

    private fun blockType(nested: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(nested))

    private fun StructBuilder.nestedBlock(fieldName: String, nestedTypeName: String, genType: IrType, valueType: IrType) {
        val nested = builderName(nestedTypeName)
        property(
            name = fieldName,
            type = genType,
            isMutable = true,
            visibility = Visibility.PUBLIC,
            initializer = rawExpr("null"),
        )
        valueSetter(fieldName, valueType)
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
            raw("${"_${fieldName}Block".escapeKotlinIdentifier()} = block")
        }
    }

    fun renderRegistration(
        fields: List<BodyFieldShape>,
        receiver: String,
        path: List<String>,
        indent: String,
    ): String = buildString {
        fields.forEach { f ->
            val fieldRef = "$receiver.${f.name.escapeKotlinIdentifier()}"
            val segs = (path + f.name).joinToString(", ") { "\"$it\"" }
            appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
            when (f) {
                is BodyFieldShape.Primitive -> Unit
                is BodyFieldShape.NestedObject ->
                    appendNestedBlock(f.name, f.typeName, f.fields, receiver, path, indent, listSegment = false)
                is BodyFieldShape.NestedList ->
                    appendNestedBlock(f.name, f.elementTypeName, f.fields, receiver, path, indent, listSegment = true)
            }
        }
    }

    private fun StringBuilder.appendNestedBlock(
        fieldName: String,
        nestedTypeName: String,
        fields: List<BodyFieldShape>,
        receiver: String,
        path: List<String>,
        indent: String,
        listSegment: Boolean,
    ) {
        val blockRef = "$receiver.${"_${fieldName}Block".escapeKotlinIdentifier()}"
        val nestedVar = "nested_$fieldName".escapeKotlinIdentifier()
        val nextPath = if (listSegment) path + fieldName + "*" else path + fieldName
        appendLine("$indent$blockRef?.let { block ->")
        appendLine("$indent    val $nestedVar = ${builderName(nestedTypeName)}().apply(block)")
        append(renderRegistration(fields, nestedVar, nextPath, "$indent    "))
        appendLine("$indent}")
    }
}

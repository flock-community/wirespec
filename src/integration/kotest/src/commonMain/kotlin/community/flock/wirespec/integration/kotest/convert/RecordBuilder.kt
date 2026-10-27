package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.StructBuilder
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.compiler.core.ir.struct
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.integration.kotest.convert.EndpointShape.BodyFieldShape
import community.flock.wirespec.compiler.core.ir.Type as IrType

internal object RecordBuilder {

    fun builderName(typeName: String): String = "${Name.of(typeName).pascalCase()}Builder"

    // Every builder pins its own fields onto a generated value with `applyTo`, and a nested block hands its
    // record to that record's builder, so the Kotlin compiler checks every pinned value against the model.
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
                is BodyFieldShape.Nested -> nestedBlock(f.name, f.record)
            }
        }
        val model = IrType.Custom(Name.of(typeName).pascalCase())
        function("applyTo") {
            visibility(Visibility.INTERNAL)
            arg("base", model)
            arg("rs", IrType.Custom("RandomSource"))
            returnType(model)
            returns(rawExpr(fields.takeIf { it.isNotEmpty() }?.let(::copyExpr) ?: "base"))
        }
    }

    private fun genNullable(element: IrType): IrType = IrType.Nullable(IrType.Custom("Gen", listOf(element)))

    private fun blockType(nested: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(nested))

    private fun blockField(fieldName: String): String = "_${fieldName}Block".escapeKotlinIdentifier()

    private fun StructBuilder.nestedBlock(fieldName: String, record: RecordRef) {
        val nested = builderName(record.typeName)
        property(
            name = fieldName,
            type = genNullable(record.valueType),
            isMutable = true,
            visibility = Visibility.PUBLIC,
            initializer = rawExpr("null"),
        )
        valueSetter(fieldName, record.valueType)
        property(
            name = "_${fieldName}Block",
            type = IrType.Nullable(blockType(nested)),
            isMutable = true,
            visibility = Visibility.PRIVATE,
            initializer = rawExpr("null"),
        )
        function("${fieldName}Block") {
            visibility(Visibility.PUBLIC)
            arg("block", blockType(nested))
            raw("${blockField(fieldName)} = block")
        }
    }

    // Builder properties are read through `this.` so a field named `base` or `rs` cannot shadow them.
    private fun copyExpr(fields: List<BodyFieldShape>): String = fields.joinToString(",\n", prefix = "base.copy(\n", postfix = ",\n)") { f ->
        val name = f.name.escapeKotlinIdentifier()
        val pinned = "this.$name"
        val base = "base.$name"
        val value = when (f) {
            is BodyFieldShape.Primitive ->
                "$pinned.let { gen -> if (gen == null) $base else ${f.refinement?.wrap("gen.draw(rs)") ?: "gen.draw(rs)"} }"
            is BodyFieldShape.Nested -> {
                val nested = "${builderName(f.record.typeName)}().apply(block)"
                // A list builds the nested builder once, not once per element.
                val viaBlock = if (f.record.isList) {
                    "?.let { block -> $nested }?.let { nested -> ${f.record.applyBuilder(base, "nested")} }"
                } else {
                    "?.let { block -> ${f.record.applyBuilder(base, nested)} }"
                }
                "$pinned?.draw(rs) ?: this.${blockField(f.name)}$viaBlock ?: $base"
            }
        }
        "  $name = $value"
    }
}

/** Where a record sits in a field or rpc outcome: on its own or as the elements of a list, either of which may be nullable. */
internal data class RecordRef(
    val typeName: String,
    val isList: Boolean,
    val isNullable: Boolean,
    val isElementNullable: Boolean,
) {
    val valueType: IrType
        get() = IrType.Custom(typeName).let { if (isList) IrType.Array(it) else it }

    /** [value], a generated value of this shape, with the record builder held by [builder] applied to every record in it. */
    fun applyBuilder(value: String, builder: String): String = when {
        isList -> "$value${if (isNullable) "?." else "."}map { e -> " +
            "${if (isElementNullable) "e?.let { $builder.applyTo(it, rs) }" else "$builder.applyTo(e, rs)"} }"
        isNullable -> "$value?.let { $builder.applyTo(it, rs) }"
        else -> "$builder.applyTo($value, rs)"
    }

    companion object {
        fun of(reference: Reference, types: Map<String, Type>): RecordRef? = when (reference) {
            is Reference.Custom -> reference.takeIf { it.value in types }
                ?.let { RecordRef(it.value, isList = false, isNullable = it.isNullable, isElementNullable = false) }
            is Reference.Iterable -> (reference.reference as? Reference.Custom)
                ?.takeIf { it.value in types }
                ?.let { RecordRef(it.value, isList = true, isNullable = reference.isNullable, isElementNullable = it.isNullable) }
            else -> null
        }
    }
}

/** How a value drawn for the inner type of a refined field (or a list or dict of them) is wrapped back into the field's type. */
internal sealed interface Refinement {
    val isNullable: Boolean

    data class Wrap(val typeName: String, override val isNullable: Boolean) : Refinement
    data class Each(val element: Refinement, override val isNullable: Boolean) : Refinement
    data class Values(val value: Refinement, override val isNullable: Boolean) : Refinement

    fun wrap(expr: String, depth: Int = 0): String {
        val v = "v$depth"
        val call = if (isNullable) "?." else "."
        return when (this) {
            is Wrap -> if (isNullable) "$expr?.let { $v -> $typeName($v) }" else "$typeName($expr)"
            is Each -> "$expr${call}map { $v -> ${element.wrap(v, depth + 1)} }"
            is Values -> "$expr${call}mapValues { (_, $v) -> ${value.wrap(v, depth + 1)} }"
        }
    }

    companion object {
        fun of(reference: Reference, refined: Map<String, Refined>): Refinement? = when (reference) {
            is Reference.Custom -> Wrap(Name.of(reference.value).pascalCase(), reference.isNullable).takeIf { reference.value in refined }
            is Reference.Iterable -> of(reference.reference, refined)?.let { Each(it, reference.isNullable) }
            is Reference.Dict -> of(reference.reference, refined)?.let { Values(it, reference.isNullable) }
            else -> null
        }
    }
}

package community.flock.wirespec.language.converter

import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.Precision
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Definition as DefinitionWirespec

fun DefinitionWirespec.convert(): Element = when (this) {
    is TypeWirespec -> convert()
    is EnumWirespec -> convert()
    is UnionWirespec -> convert()
    is RefinedWirespec -> convert()
    else -> error("Conversion not implemented for ${this::class.simpleName}")
}

fun TypeWirespec.convert() = Struct(
    name = this.identifier.value,
    fields = this.shape.value.map {
        Field(
            name = it.identifier.value,
            type = it.reference.convert(),
        )
    },
    interfaces = extends.map { it.convert() }.filterIsInstance<Type.Custom>()
)

fun EnumWirespec.convert() = Enum(
    name = this.identifier.value,
    extends = Type.Custom("Wirespec.Enum"),
    entries = this.entries.map { Enum.Entry(it, emptyList()) }
)

fun UnionWirespec.convert() = Union(
    name = this.identifier.value,
    members = this.entries.map { it.convert() }.filterIsInstance<Type.Custom>().map { it.name }
)

fun RefinedWirespec.convert() = Struct(
    name = this.identifier.value,
    fields = listOf(
        Field(
            name = "value",
            type = this.reference.convert(),
        )
    ),
    interfaces = listOf(Type.Custom("Wirespec.Refined"))
)

fun ReferenceWirespec.convert(): Type =
    when (this) {
        is ReferenceWirespec.Any -> TODO("Any is not implemented yet")
        is ReferenceWirespec.Custom -> Type.Custom(value)
        is ReferenceWirespec.Dict -> Type.Dict(Type.String, reference.convert())
        is ReferenceWirespec.Iterable -> Type.Array(reference.convert())
        is ReferenceWirespec.Primitive -> when (val t = type) {
            ReferenceWirespec.Primitive.Type.Boolean -> Type.Boolean
            ReferenceWirespec.Primitive.Type.Bytes -> Type.Bytes
            is ReferenceWirespec.Primitive.Type.Integer -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Integer(Precision.P32)
                ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Integer(Precision.P64)
            }
            is ReferenceWirespec.Primitive.Type.Number -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Number(Precision.P32)
                ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Number(Precision.P64)
            }
            is ReferenceWirespec.Primitive.Type.String -> Type.String
        }
        is ReferenceWirespec.Unit -> Type.Unit
    }
        .let { if (isNullable) Type.Nullable(it) else it }
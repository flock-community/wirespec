package community.flock.wirespec.language.converter


import community.flock.wirespec.language.core.Call
import community.flock.wirespec.language.core.Case
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.Function
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Precision
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.compiler.core.parse.ast.Channel as ChannelWirespec
import community.flock.wirespec.compiler.core.parse.ast.Definition as DefinitionWirespec
import community.flock.wirespec.compiler.core.parse.ast.Endpoint as EndpointWirespec
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

fun DefinitionWirespec.convert(): Element = when (this) {
    is TypeWirespec -> convert()
    is EnumWirespec -> convert()
    is UnionWirespec -> convert()
    is RefinedWirespec -> convert()
    is ChannelWirespec -> convert()
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
    interfaces = extends.map { it.convert() }.filterIsInstance<Type.Custom>(),
)

fun EnumWirespec.convert() = Enum(
    name = this.identifier.value,
    extends = Type.Custom("Wirespec.Enum"),
    entries = this.entries.map { Enum.Entry(it, emptyList()) },
)

fun UnionWirespec.convert() = Union(
    name = this.identifier.value,
    members = this.entries.map { it.convert() }.filterIsInstance<Type.Custom>().map { it.name },
)

fun RefinedWirespec.convert() = Struct(
    name = this.identifier.value,
    fields = listOf(
        Field(
            name = "value",
            type = this.reference.convert(),
        ),
    ),
    interfaces = listOf(Type.Custom("Wirespec.Refined")),
)

fun ChannelWirespec.convert() = Interface(
    name = identifier.value,
    extends = Type.Custom("Wirespec.Channel"),
    elements = listOf(
        Function(
            name = "invoke",
            parameters = listOf(
                Parameter(
                    name = "message",
                    type = reference.convert(),
                )
            ),
            returnType = Type.Unit,
            body = emptyList(),
            isAsync = false,
            isStatic = false,
            isOverride = false,
        )
    )
)

fun ReferenceWirespec.convert(): Type = when (this) {
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


fun EndpointWirespec.convertFromResponse() = Function(
    name = "fromResponse",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Deserializer")),
        Parameter("response", Type.Custom("Wirespec.RawResponse"))
    ),
    returnType = Type.Custom("Response", listOf(Type.Custom("?"))),
    body = listOf(
        Switch(
            expression = RawExpression("response.statusCode()"),
            cases = responses.distinctBy { it.status }.filter { it.status.toIntOrNull() != null }.map { response ->
                Case(
                    value = Literal(response.status.toInt(), Type.Integer(Precision.P32)),
                    body = listOf(
                        ReturnStatement(
                            ConstructorStatement(
                                type = Type.Custom("Response${response.status}"),
                                namedArguments = mapOf(
                                    "headers" to ConstructorStatement(
                                        type = Type.Custom("Headers"),
                                        namedArguments = response.headers.associate { header ->
                                            header.identifier.value to Call(
                                                name = "serialization.deserializeParam",
                                                arguments = mapOf(
                                                    "value" to RawExpression("response.headers().getOrDefault(\"${header.identifier.value}\", java.util.Collections.emptyList())"),
                                                    "type" to header.reference.toJavaType()
                                                )
                                            )
                                        }
                                    ),
                                    "body" to (response.content?.let { content ->
                                        Call(
                                            name = "serialization.deserializeBody",
                                            arguments = mapOf(
                                                "value" to RawExpression("response.body()"),
                                                "type" to content.reference.toJavaType()
                                            )
                                        )
                                    } ?: RawExpression("null"))
                                )
                            )
                        )
                    )
                )
            },
            default = listOf(
                ErrorStatement(RawExpression("\"Cannot match response with status: \" + response.statusCode()"))
            )
        )
    )
)

fun EndpointWirespec.convertToRequest() = Function(
    name = "toRequest",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Serializer")),
        Parameter("request", Type.Custom("Request"))
    ),
    returnType = Type.Custom("Wirespec.RawRequest"),
    body = listOf(
        ReturnStatement(
            ConstructorStatement(
                type = Type.Custom("Wirespec.RawRequest"),
                namedArguments = mapOf<String, Expression>(
                    "method" to RawExpression("request.method().name()"),
                    "path" to LiteralList(
                        values = path.map {
                            when (it) {
                                is EndpointWirespec.Segment.Literal -> Literal(it.value, Type.String)
                                is EndpointWirespec.Segment.Param -> Call(
                                    name = "serialization.serializePath",
                                    arguments = mapOf(
                                        "value" to RawExpression("request.path().${it.identifier.value.replaceFirstChar { char -> char.lowercase() }}()"),
                                        "type" to it.reference.toJavaType()
                                    )
                                )
                            }
                        },
                        type = Type.String
                    ),
                    "queries" to LiteralMap(
                        values = queries.associate {
                            it.identifier.value to Call(
                                name = "serialization.serializeParam",
                                arguments = mapOf(
                                    "value" to RawExpression("request.queries().${it.identifier.value}()"),
                                    "type" to it.reference.toJavaType()
                                )
                            )
                        },
                        keyType = Type.String,
                        valueType = Type.Custom("List<String>")
                    ),
                    "headers" to LiteralMap(
                        values = headers.associate {
                            it.identifier.value to Call(
                                name = "serialization.serializeParam",
                                arguments = mapOf(
                                    "value" to RawExpression("request.headers().${it.identifier.value}()"),
                                    "type" to it.reference.toJavaType()
                                )
                            )
                        },
                        keyType = Type.String,
                        valueType = Type.Custom("List<String>")
                    ),
                    "body" to (requests.first().content?.let {
                        Call(
                            name = "serialization.serializeBody",
                            arguments = mapOf(
                                "value" to RawExpression("request.body()"),
                                "type" to it.reference.toJavaType()
                            )
                        )
                    } ?: RawExpression("null"))
                )
            )
        )
    )
)

fun EndpointWirespec.convertFromRequest() = Function(
    name = "fromRequest",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Deserializer")),
        Parameter("request", Type.Custom("Wirespec.RawRequest"))
    ),
    returnType = Type.Custom("Request"),
    body = listOf(
        ReturnStatement(
            ConstructorStatement(
                type = Type.Custom("Request"),
                namedArguments = mapOf<String, Expression>()
                    .plus(path.mapIndexedNotNull { index, segment ->
                        if (segment is EndpointWirespec.Segment.Param) {
                            segment.identifier.value to Call(
                                name = "serialization.deserializePath",
                                arguments = mapOf(
                                    "value" to RawExpression("request.path().get($index)"),
                                    "type" to segment.reference.toJavaType()
                                )
                            )
                        } else null
                    })
                    .plus(queries.map { field ->
                        field.identifier.value to Call(
                            name = "serialization.deserializeParam",
                            arguments = mapOf(
                                "value" to RawExpression("request.queries().getOrDefault(\"${field.identifier.value}\", java.util.Collections.emptyList())"),
                                "type" to field.reference.toJavaType()
                            )
                        )
                    })
                    .plus(headers.map { field ->
                        field.identifier.value to Call(
                            name = "serialization.deserializeParam",
                            arguments = mapOf(
                                "value" to RawExpression("request.headers().getOrDefault(\"${field.identifier.value}\", java.util.Collections.emptyList())"),
                                "type" to field.reference.toJavaType()
                            )
                        )
                    })
                    .plus(requests.first().content?.let {
                        mapOf("body" to Call(
                            name = "serialization.deserializeBody",
                            arguments = mapOf(
                                "value" to RawExpression("request.body()"),
                                "type" to it.reference.toJavaType()
                            )
                        ))
                    } ?: emptyMap())
            )
        )
    )
)

private fun ReferenceWirespec.toJavaType(): Expression {
    val isIterable = this is ReferenceWirespec.Iterable
    val isNullable = this.isNullable

    val element = if (isIterable) (this as ReferenceWirespec.Iterable).reference else this

    val elementClass = when (element) {
        is ReferenceWirespec.Custom -> element.value
        is ReferenceWirespec.Primitive -> when (val t = element.type) {
            is ReferenceWirespec.Primitive.Type.String -> "String"
            is ReferenceWirespec.Primitive.Type.Integer -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> "Integer"
                ReferenceWirespec.Primitive.Type.Precision.P64 -> "Long"
            }
            is ReferenceWirespec.Primitive.Type.Number -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> "Float"
                ReferenceWirespec.Primitive.Type.Precision.P64 -> "Double"
            }
            is ReferenceWirespec.Primitive.Type.Boolean -> "Boolean"
            is ReferenceWirespec.Primitive.Type.Bytes -> "byte[]"
        }
        else -> "Void"
    }

    val containerClass = if (isNullable) "java.util.Optional.class" else if (isIterable) "java.util.List.class" else "null"

    return RawExpression("Wirespec.getType($elementClass.class, $containerClass)")
}

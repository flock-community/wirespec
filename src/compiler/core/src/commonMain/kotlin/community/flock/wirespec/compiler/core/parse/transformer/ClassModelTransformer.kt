package community.flock.wirespec.compiler.core.parse.transformer

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.nodes.ClassModel
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.EnumClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.RefinedClass
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.TypeClass

object ClassModelTransformer {

    fun transform(ast: List<Definition>): List<ClassModel> {
        return ast.map {
            when (it) {
                is Endpoint -> it.transform()
                is Enum -> it.transform()
                is Refined -> it.transform()
                is Type -> it.transform()
            }
        }
    }

    private fun Type.transform(): TypeClass =
        TypeClass(
            name = name,
            fields = shape.value.map {
                Field(
                    identifier = it.identifier.value,
                    reference = it.reference.transform(it.isNullable, false),
                )
            }
        )

    private fun Refined.transform(): RefinedClass =
        RefinedClass(
            name = name,
            validator = RefinedClass.Validator(
                value = validator.value
            )
        )

    private fun Type.Shape.Field.Reference.transform(isNullable: Boolean, isOptional: Boolean): Reference =
        when (this) {
            is Type.Shape.Field.Reference.Any -> Reference.Language(
                primitive = Reference.Language.Primitive.Any,
                isNullable = isNullable,
                isOptional = isOptional,
                isIterable = isIterable,
            )

            is Type.Shape.Field.Reference.Custom -> Reference.Custom(
                name = value,
                isNullable = isNullable,
                isOptional = isOptional,
                isIterable = isIterable,
            )

            is Type.Shape.Field.Reference.Primitive -> when (type) {
                Type.Shape.Field.Reference.Primitive.Type.String -> Reference.Language(
                    primitive = Reference.Language.Primitive.String,
                    isNullable = isNullable,
                    isOptional = isOptional,
                    isIterable = isIterable,
                )

                Type.Shape.Field.Reference.Primitive.Type.Integer -> Reference.Language(
                    primitive = Reference.Language.Primitive.Long,
                    isNullable = isNullable,
                    isOptional = isOptional,
                    isIterable = isIterable,
                )

                Type.Shape.Field.Reference.Primitive.Type.Number -> Reference.Language(
                    primitive = Reference.Language.Primitive.Double,
                    isNullable = isNullable,
                    isOptional = isOptional,
                    isIterable = isIterable,
                )

                Type.Shape.Field.Reference.Primitive.Type.Boolean -> Reference.Language(
                    primitive = Reference.Language.Primitive.Boolean,
                    isNullable = isNullable,
                    isOptional = isOptional,
                    isIterable = isIterable,
                )
            }

            is Type.Shape.Field.Reference.Unit -> Reference.Language(
                primitive = Reference.Language.Primitive.Unit,
                isNullable = isNullable,
                isOptional = isOptional,
                isIterable = isIterable,
            )
        }

    private fun Enum.transform(): EnumClass =
        EnumClass(
            name = name,
            entries = entries
        )

    private fun Endpoint.transform(): EndpointClass {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return EndpointClass(
            name = "${name}Endpoint",
            path = path.joinToString("/") {
                when (it) {
                    is Endpoint.Segment.Literal -> it.value
                    is Endpoint.Segment.Param -> "{${it.identifier.transform()}}"
                }
            },
            method = method.name,
            requestClasses = requests.map {
                EndpointClass.RequestClass(
                    name = "Request${it.content.name()}",
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Custom("String", false)
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Custom("Wirespec.Method", false)
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Custom(
                                name = "Map",
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Custom(
                                            name = "List",
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Custom("Any", true)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Custom(
                                name = "Map",
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Custom(
                                            name = "List",
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Custom("Any", true)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        it.content?.reference?.transform()
                                            ?: Reference.Language(Reference.Language.Primitive.Unit)
                                    )
                                )
                            )
                        ),
                    ),
                    primaryConstructor = EndpointClass.RequestClass.PrimaryConstructor(
                        name = "${name}Endpoint",
                        parameters = listOf(
                            Parameter(
                                identifier = "path",
                                reference = Reference.Custom("String", false)
                            ),
                            Parameter(
                                identifier = "method",
                                reference = Reference.Custom("Wirespec.Method", false)
                            ),
                            Parameter(
                                identifier = "query",
                                reference = Reference.Custom(
                                    name = "Map",
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Custom(
                                                name = "List",
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Custom("Any", true)
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Custom(
                                    name = "Map",
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Custom(
                                                name = "List",
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Custom("Any", true)
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            Parameter(
                                identifier = "content",
                                reference = Reference.Custom(
                                    name = "Wirespec.Content",
                                    isNullable = true,
                                    generics = Reference.Generics(
                                        listOf(
                                            it.content?.reference?.transform()
                                                ?: Reference.Language(Reference.Language.Primitive.Unit)
                                        )
                                    )
                                )
                            ),
                        ),
                    ),
                    secondaryConstructor = EndpointClass.RequestClass.SecondaryConstructor(
                        name = "${name}Endpoint",
                        parameters = parameters.map { it.transformParameter() },
                        path = EndpointClass.Path(path.map { it.transform() }),
                        method = method.name,
                        query = "",
                        headers = "",
                    ),
                    supers = emptyList()
                )
            },
            requestMapper = EndpointClass.RequestMapper(
                name = "REQUEST_MAPPER",
                conditions = requests.map {
                    EndpointClass.RequestMapper.RequestCondition(
                        content = it.content?.transform(),
                        isIterable = it.content?.reference?.isIterable ?: false,
                        responseReference = Reference.Custom("Request${it.content.name()}")
                    )
                }
            ),
            responseMapper = EndpointClass.ResponseMapper(
                name = "RESPONSE_MAPPER",
                conditions = responses.map {
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = it.status,
                        content = it.content?.transform(),
                        isIterable = it.content?.reference?.isIterable ?: false,
                        responseReference = Reference.Custom("Response${it.status}${it.content.name()}")
                    )
                }
            ),
            responseInterfaces = responses.map {
                EndpointClass.ResponseInterface(
                    name = Reference.Custom(
                        name = "Response${it.status}",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    ),
                    `super` = Reference.Custom(
                        name = "Response",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    )
                )
            },
            responseClasses = responses.map {
                EndpointClass.ResponseClass(
                    name = "Response${it.status}${it.content.name()}",
                    fields = listOf(
                        Field(
                            identifier = "status",
                            reference = Reference.Language(
                                primitive = Reference.Language.Primitive.Integer
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Reference.Language.Primitive.Map,
                                isNullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Reference.Language.Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(
                                                        primitive = Reference.Language.Primitive.Any,
                                                        isNullable = true
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        it.content?.reference?.transform()
                                            ?: Reference.Language(Reference.Language.Primitive.Unit)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        )
                    ),
                    allArgsConstructor = EndpointClass.ResponseClass.AllArgsConstructor(
                        name = "Response${it.status}${it.content.name()}",
                        statusCode = it.status,
                        parameters = listOf(
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Reference.Language.Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Reference.Language.Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Reference.Language.Primitive.Any,
                                                            isNullable = true
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            Parameter(
                                identifier = "body",
                                reference = it.content?.reference?.transform()
                                    ?: Reference.Language(Reference.Language.Primitive.Unit)
                            )
                        ),
                        content = it.content?.transform()
                    ),
                    returnReference = Reference.Custom(
                        name = "Response${it.status}",
                        generics = Reference.Generics(
                            listOf(
                                it.content?.reference?.transform()
                                    ?: Reference.Language(Reference.Language.Primitive.Unit)
                            )
                        ),
                        isNullable = false,
                    ),
                    statusCode = it.status,
                    content = it.content?.transform()
                )
            },
            supers = emptyList()
        )
    }

    private fun Type.Shape.Field.transform() =
        Field(
            identifier = this.identifier.value,
            reference = this.reference.transform(),
            isPrivate = false,
            isFinal = false,
            isOverride = false,
        )

    private fun Type.Shape.Field.transformParameter() =
        Parameter(
            identifier = this.identifier.value,
            reference = this.reference.transform(),
        )

    private fun Type.Shape.Field.Identifier.transform() =
        value
            .split("-", ".")
            .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
            .joinToString("")

    private fun Endpoint.Content?.name() = this?.type
        ?.substringBefore(";")
        ?.split("/", "-")
        ?.joinToString("") { it.firstToUpper() }
        ?.replace("+", "")
        ?: "Unit"

    private fun Endpoint.Content.transform() = EndpointClass.Content(
        type = this.type,
        reference = this.reference.transform()
    )

    private fun Type.Shape.Field.Reference.transform() =
        when (this) {
            is Type.Shape.Field.Reference.Unit -> "Unit"
            is Type.Shape.Field.Reference.Any -> "Any"
            is Type.Shape.Field.Reference.Custom -> value
            is Type.Shape.Field.Reference.Primitive -> when (type) {
                Type.Shape.Field.Reference.Primitive.Type.String -> "String"
                Type.Shape.Field.Reference.Primitive.Type.Integer -> "Long"
                Type.Shape.Field.Reference.Primitive.Type.Number -> "Double"
                Type.Shape.Field.Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }
            .let {
                Reference.Custom(it)
            }
            .let {
                if (isIterable) Reference.Custom(name = "List", generics = Reference.Generics(listOf(it)))
                else it
            }
            .let {
                if (isMap) Reference.Custom(name = "Map", generics = Reference.Generics(listOf(it)))
                else it
            }

    private fun Endpoint.Segment.transform() =
        when (this) {
            is Endpoint.Segment.Literal -> EndpointClass.Path.Literal(value)
            is Endpoint.Segment.Param -> EndpointClass.Path.Parameter(identifier.value)
        }

}
package community.flock.wirespec.compiler.core.parse.transformer

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.isInt
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.isStatusCode
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

    fun transform(ast: List<Definition>): List<ClassModel> = ast.map {
        when (it) {
            is Endpoint -> it.transform()
            is Enum -> it.transform()
            is Refined -> it.transform()
            is Type -> it.transform()
        }
    }

    private fun Type.transform(): TypeClass =
        TypeClass(
            name = className(name),
            fields = shape.value.map {
                Field(
                    identifier = it.identifier.value,
                    reference = it.reference.transform(false, it.isNullable),
                )
            }
        )

    private fun Refined.transform(): RefinedClass =
        RefinedClass(
            name = className(name),
            validator = RefinedClass.Validator(
                value = validator.value
            )
        )

    private fun Enum.transform(): EnumClass =
        EnumClass(
            name = className(name),
            entries = entries
        )

    private fun Endpoint.transform(): EndpointClass {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return EndpointClass(
            name = className(name, "Endpoint"),
            path = path.joinToString("/", "/") {
                when (it) {
                    is Endpoint.Segment.Literal -> it.value
                    is Endpoint.Segment.Param -> "{${it.identifier.transform()}}"
                }
            },
            functionName = name.firstToLower(),
            method = method.name,
            requestClasses = requests.map {
                EndpointClass.RequestClass(
                    name = className("Request", it.content.name()),
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Language(
                                primitive = Reference.Language.Primitive.String,
                                isNullable = false,
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true,
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Wirespec("Method"),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true,
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                primitive = Reference.Language.Primitive.Map,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Language(
                                            primitive = Reference.Language.Primitive.String,
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
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true,
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Reference.Language.Primitive.Map,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Language(
                                            primitive = Reference.Language.Primitive.String,
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
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true,
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = it.content == null,
                                generics = Reference.Generics(
                                    listOf(
                                        it.content?.reference?.transform(it.content.isNullable, false)
                                            ?: Reference.Language(Reference.Language.Primitive.Unit)
                                    )
                                )
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true,
                        ),
                    ),
                    requestAllArgsConstructor = EndpointClass.RequestClass.RequestAllArgsConstructor(
                        name = className("Request", it.content.name()),
                        parameters = listOf(
                            Parameter(
                                identifier = "path",
                                reference = Reference.Language(
                                    primitive = Reference.Language.Primitive.String,
                                    isNullable = false
                                )
                            ),
                            Parameter(
                                identifier = "method",
                                reference = Reference.Wirespec("Method", false)
                            ),
                            Parameter(
                                identifier = "query",
                                reference = Reference.Language(
                                    primitive = Reference.Language.Primitive.Map,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Language(
                                                primitive = Reference.Language.Primitive.String,
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
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Reference.Language.Primitive.Map,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Language(
                                                primitive = Reference.Language.Primitive.String,
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
                                identifier = "content",
                                reference = Reference.Wirespec(
                                    name = "Content",
                                    isNullable = it.content == null,
                                    generics = Reference.Generics(
                                        listOf(
                                            it.content?.reference?.transform(it.content.isNullable, false)
                                                ?: Reference.Language(Reference.Language.Primitive.Unit)
                                        )
                                    )
                                )
                            ),
                        ),
                    ),
                    requestParameterConstructor = EndpointClass.RequestClass.RequestParameterConstructor(
                        name = className("Request", it.content.name()),
                        parameters = parameters
                            .plus(it.content?.reference?.toField("body", false))
                            .filterNotNull()
                            .map { it.transformParameter() },
                        content = it.content?.transform(),
                        path = EndpointClass.Path(path.map { it.transform() }),
                        method = method.name,
                        query = query.map { it.identifier.value },
                        headers = headers.map { it.identifier.value },
                    ),
                    supers = listOf(
                        Reference.Custom(
                            name = className("Request"),
                            generics = Reference.Generics(
                                listOf(
                                    it.content?.reference?.transform(it.content.isNullable, false)
                                        ?: Reference.Language(Reference.Language.Primitive.Unit)
                                )
                            )
                        )
                    )
                )
            },
            requestMapper = EndpointClass.RequestMapper(
                name = "REQUEST_MAPPER",
                conditions = requests.map {
                    EndpointClass.RequestMapper.RequestCondition(
                        content = it.content?.transform(),
                        isIterable = it.content?.reference?.isIterable ?: false,
                        responseReference = Reference.Custom(className("Request", it.content.name()))
                    )
                }
            ),
            responseMapper = EndpointClass.ResponseMapper(
                name = "RESPONSE_MAPPER",
                conditions = responses.sortedBy { it.status }.map {
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = it.status,
                        content = it.content?.transform(),
                        isIterable = it.content?.reference?.isIterable ?: false,
                        responseReference = Reference.Custom(
                            name = className("Response", it.status, it.content.name())
                        )
                    )
                }
            ),
            responseInterfaces = responses
                .map { it.status.groupStatus() }
                .distinct()
                .map {
                    EndpointClass.ResponseInterface(
                        name = Reference.Custom(
                            name = className("Response", it),
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
                } + responses
                .filter { it.status.isStatusCode() }
                .map { it.status }
                .distinct()
                .map {
                    EndpointClass.ResponseInterface(
                        name = Reference.Custom(
                            name = className("Response", it),
                            generics = Reference.Generics(
                                listOf(
                                    Reference.Custom("T")
                                )
                            )
                        ),
                        `super` = Reference.Custom(
                            name = className("Response", it.groupStatus()),
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
                    name = className("Response", it.status, it.content.name()),
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
                                        Reference.Language(
                                            primitive = Reference.Language.Primitive.String,
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
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = it.content == null,
                                generics = Reference.Generics(
                                    listOf(
                                        it.content?.reference?.transform(it.content.isNullable, false)
                                            ?: Reference.Language(Reference.Language.Primitive.Unit)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        )
                    ),
                    responseAllArgsConstructor = EndpointClass.ResponseClass.ResponseAllArgsConstructor(
                        name = className("Response", it.status, it.content.name()),
                        statusCode = it.status,
                        parameters = listOf(
                            Parameter(
                                identifier = "status",
                                reference = Reference.Language(Reference.Language.Primitive.Integer)
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Reference.Language.Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Language(
                                                primitive = Reference.Language.Primitive.String,
                                                isNullable = false,
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
                                identifier = "content",
                                reference = Reference.Wirespec(
                                    name = "Content",
                                    isNullable = it.content == null,
                                    generics = Reference.Generics(
                                        listOf(
                                            it.content?.reference?.transform(it.content.isNullable, false)
                                                ?: Reference.Language(Reference.Language.Primitive.Unit)
                                        )
                                    )
                                )
                            )
                        ),
                        content = it.content?.transform()
                    ),
                    responseParameterConstructor = EndpointClass.ResponseClass.ResponseParameterConstructor(
                        name = className("Response", it.status, it.content.name()),
                        statusCode = it.status,
                        parameters = listOf(if(!it.status.isInt()) Parameter("status", Reference.Language(Reference.Language.Primitive.Integer)) else null)
                            .plus(it.headers.map { it.transformParameter() },)
                            .plus(it.content?.reference?.toField("body")?.transformParameter())
                            .filterNotNull(),
                        headers = it.headers.map { it.identifier.value },
                        content = it.content?.transform(),
                    ),
                    `super` = Reference.Custom(
                        name = className("Response", it.status),
                        generics = Reference.Generics(
                            listOf(
                                it.content?.reference?.transform(it.content.isNullable, false)
                                    ?: Reference.Language(Reference.Language.Primitive.Unit)
                            )
                        ),
                        isNullable = false,
                    ),
                    statusCode = it.status,
                    content = it.content?.transform(),
                )
            },
            supers = listOf(
                Reference.Wirespec(
                    name = "Endpoint"
                )
            )
        )
    }

    private fun Type.Shape.Field.transform() =
        Field(
            identifier = this.identifier.value,
            reference = this.reference.transform(isNullable, false),
            isPrivate = false,
            isFinal = false,
            isOverride = false,
        )

    private fun Type.Shape.Field.transformParameter() =
        Parameter(
            identifier = identifier.value,
            reference = reference.transform(false, isNullable),
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
        reference = this.reference.transform(isNullable, false),
    )

    private fun Type.Shape.Field.Reference.transform(isNullable: Boolean, isOptional: Boolean) =
        when (this) {
            is Type.Shape.Field.Reference.Unit -> Reference.Language(
                Reference.Language.Primitive.Unit,
                isNullable,
                isIterable,
                isOptional
            )

            is Type.Shape.Field.Reference.Any -> Reference.Language(
                Reference.Language.Primitive.Any,
                isNullable,
                isIterable,
                isOptional
            )

            is Type.Shape.Field.Reference.Custom -> Reference.Custom(value, isNullable, isIterable, isOptional)
            is Type.Shape.Field.Reference.Primitive ->
                when (type) {
                    Type.Shape.Field.Reference.Primitive.Type.String -> Reference.Language.Primitive.String
                    Type.Shape.Field.Reference.Primitive.Type.Integer -> Reference.Language.Primitive.Long
                    Type.Shape.Field.Reference.Primitive.Type.Number -> Reference.Language.Primitive.Double
                    Type.Shape.Field.Reference.Primitive.Type.Boolean -> Reference.Language.Primitive.Boolean
                }.let { Reference.Language(it, isNullable, isIterable, isOptional) }
        }

    private fun Endpoint.Segment.transform() =
        when (this) {
            is Endpoint.Segment.Literal -> EndpointClass.Path.Literal(value)
            is Endpoint.Segment.Param -> EndpointClass.Path.Parameter(identifier.value)
        }

    private fun className(vararg args: String): String =
        args.joinToString("") {
            it.firstToUpper()
        }

    private fun Type.Shape.Field.Reference.toField(identifier: String, isNullable: Boolean = false) = Type.Shape.Field(
        Type.Shape.Field.Identifier(identifier),
        this,
        isNullable
    )

    private fun String.groupStatus() =
        if (isInt()) substring(0, 1) + "XX"
        else firstToUpper()

}
package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.EnumClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Reference.Language.Primitive
import community.flock.wirespec.compiler.core.parse.nodes.RefinedClass
import community.flock.wirespec.compiler.core.parse.nodes.TypeClass

object ClassModelFixture {

    val refined =
        RefinedClass(
            name = "UUID",
            validator = RefinedClass.Validator(
                "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$"
            )
        )


    val enum =
        EnumClass(
            name = "TodoStatus",
            entries = setOf(
                "OPEN",
                "IN_PROGRESS",
                "CLOSE",
            )
        )


    val type =
        TypeClass(
            name = "Todo",
            fields = listOf(
                Field(
                    identifier = "name",
                    reference = Reference.Language(
                        primitive = Primitive.String
                    )
                ),
                Field(
                    identifier = "description",
                    reference = Reference.Language(
                        primitive = Primitive.String,
                        isOptional = true,
                    ),
                ),
                Field(
                    identifier = "notes",
                    reference = Reference.Language(
                        primitive = Primitive.String,
                        isIterable = true
                    ),
                ),
                Field(
                    identifier = "done",
                    reference = Reference.Language(
                        primitive = Primitive.Boolean
                    )
                )

            )
        )


    val endpoint =
        EndpointClass(
            name = "AddPetEndpoint",
            functionName = "addPet",
            path = "/pet",
            method = "POST",
            supers = listOf(
                Reference.Wirespec("Endpoint")
            ),
            requestClasses = listOf(
                EndpointClass.RequestClass(
                    name = "RequestApplicationXml",
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "String",
                                            isNullable = false
                                        ),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(
                                                        primitive = Primitive.Any,
                                                        isNullable = true
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "String",
                                            isNullable = false
                                        ),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                    ),
                    requestAllArgsConstructor = EndpointClass.RequestClass.RequestAllArgsConstructor(
                        name = "RequestApplicationXml",
                        parameters = listOf(
                            Parameter(
                                identifier = "path",
                                reference = Reference.Custom(
                                    name = "String",
                                    isNullable = false,
                                    isOptional = false,
                                ),
                            ),
                            Parameter(
                                identifier = "method",
                                reference = Reference.Wirespec(
                                    name = "Method",
                                    isNullable = false,
                                    isOptional = false,
                                ),
                            ),
                            Parameter(
                                identifier = "query",
                                reference = Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    isOptional = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                "headers", Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    isOptional = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                "content", Reference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    isOptional = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "Pet",
                                                isNullable = false
                                            )
                                        )
                                    )
                                )
                            ),
                        ),
                    ),
                    requestParameterConstructor = EndpointClass.RequestClass.RequestParameterConstructor(
                        name = "RequestApplicationXml",
                        parameters = listOf(
                            Parameter("body", Reference.Custom("Pet", false)),
                        ),
                        path = EndpointClass.Path(
                            listOf(
                                EndpointClass.Path.Literal("pet")
                            )
                        ),
                        method = "POST",
                        query = emptyList(),
                        headers = emptyList(),
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            name = "Request",
                            isNullable = false,
                            generics = Reference.Generics(
                                listOf(
                                    Reference.Custom(
                                        name = "Pet",
                                        isNullable = false
                                    )
                                )
                            )
                        )
                    )
                ),
                EndpointClass.RequestClass(
                    name = "RequestApplicationJson",
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "String", isNullable = false
                                        ),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(
                                                        primitive = Primitive.Any,
                                                        isNullable = true
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                    ),
                    requestAllArgsConstructor =
                    EndpointClass.RequestClass.RequestAllArgsConstructor(
                        name = "RequestApplicationJson",
                        parameters = listOf(
                            Parameter("path", Reference.Custom("String", false)),
                            Parameter("method", Reference.Wirespec("Method", false)),
                            Parameter(
                                "query", Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                "headers", Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String", false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                "content", Reference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "Pet",
                                                isNullable = false
                                            )
                                        )
                                    )
                                )
                            ),
                        ),

                        ),
                    requestParameterConstructor = EndpointClass.RequestClass.RequestParameterConstructor(
                        name = "RequestApplicationJson",
                        parameters = listOf(
                            Parameter(
                                "body", Reference.Custom(
                                    name = "Pet",
                                    isNullable = false
                                )
                            ),
                        ),
                        path = EndpointClass.Path(
                            listOf(
                                EndpointClass.Path.Literal("pet")
                            )
                        ),
                        method = "POST",
                        query = emptyList(),
                        headers = emptyList(),
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            name = "Request",
                            isNullable = false,
                            generics = Reference.Generics(
                                listOf(
                                    Reference.Custom(
                                        name = "Pet",
                                        isNullable = false
                                    )
                                )
                            )
                        )
                    )
                ),
                EndpointClass.RequestClass(
                    name = "RequestApplicationXWwwFormUrlencoded",
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                    ),
                    requestAllArgsConstructor = EndpointClass.RequestClass.RequestAllArgsConstructor(
                        name = "RequestApplicationXWwwFormUrlencoded",
                        parameters = listOf(
                            Parameter("path", Reference.Custom("String", false)),
                            Parameter("method", Reference.Wirespec("Method", false)),
                            Parameter(
                                identifier = "query", Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                "headers", Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(Primitive.Any, true)
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            Parameter(
                                "content", Reference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "Pet",
                                                isNullable = false
                                            )
                                        )
                                    )
                                )
                            ),
                        ),

                        ),
                    requestParameterConstructor = EndpointClass.RequestClass.RequestParameterConstructor(
                        name = "RequestApplicationXWwwFormUrlencoded",
                        parameters = listOf(
                            Parameter("body", Reference.Custom("Pet", false)),
                        ),
                        path = EndpointClass.Path(
                            listOf(
                                EndpointClass.Path.Literal("pet")
                            )
                        ),
                        method = "POST",
                        query = emptyList(),
                        headers = emptyList(),
                        content = EndpointClass.Content(
                            type = "application/x-www-form-urlencoded",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            name = "Request",
                            isNullable = false,
                            generics = Reference.Generics(
                                listOf(
                                    Reference.Custom(
                                        name = "Pet",
                                        isNullable = false
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            responseInterfaces = listOf(
                EndpointClass.ResponseInterface(
                    name = Reference.Custom(
                        name = "Response2XX",
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
                ),
                EndpointClass.ResponseInterface(
                    Reference.Custom(
                        name = "Response4XX",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    ),
                    Reference.Custom(
                        name = "Response",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    )
                ),
                EndpointClass.ResponseInterface(
                    Reference.Custom(
                        name = "Response200",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    ),
                    Reference.Custom(
                        name = "Response2XX",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    )
                ),
                EndpointClass.ResponseInterface(
                    Reference.Custom(
                        name = "Response405",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    ),
                    Reference.Custom(
                        name = "Response4XX",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom("T")
                            )
                        )
                    )
                ),
            ),
            responseClasses = listOf(
                EndpointClass.ResponseClass(
                    name = "Response200ApplicationXml",
                    fields = listOf(
                        Field(
                            identifier = "status",
                            reference = Reference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
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
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "Pet",
                                            isNullable = false,
                                        ),
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        )
                    ),
                    responseAllArgsConstructor = EndpointClass.ResponseClass.ResponseAllArgsConstructor(
                        name = "Response200ApplicationXml",
                        statusCode = "200",
                        parameters = listOf(
                            Parameter(
                                identifier = "status",
                                reference = Reference.Language(
                                    primitive = Primitive.Integer
                                )
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                    isNullable = true,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "Pet",
                                                isNullable = false,
                                            ),
                                        )
                                    )
                                ),
                            )
                        ),
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    `super` = Reference.Custom(
                        name = "Response200",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        isNullable = false,
                    ),
                    statusCode = "200",
                    content = EndpointClass.Content(
                        type = "application/xml",
                        reference = Reference.Custom(
                            name = "Pet",
                            isNullable = false,
                        ),
                    ),
                ),
                EndpointClass.ResponseClass(
                    name = "Response200ApplicationJson",
                    statusCode = "200",
                    fields = listOf(
                        Field(
                            identifier = "status",
                            reference = Reference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
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
                                isNullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "Pet",
                                            isNullable = false,
                                        ),
                                    )
                                )
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true
                        )
                    ),
                    responseAllArgsConstructor = EndpointClass.ResponseClass.ResponseAllArgsConstructor(
                        name = "Response200ApplicationJson",
                        statusCode = "200",
                        parameters = listOf(
                            Parameter(
                                identifier = "status",
                                reference = Reference.Language(
                                    primitive = Primitive.Integer
                                ),
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "Pet",
                                                isNullable = false,
                                            ),
                                        )
                                    )
                                )
                            ),
                        ),
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    `super` = Reference.Custom(
                        name = "Response200",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        isNullable = false,
                    ),
                    content = EndpointClass.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            name = "Pet",
                            isNullable = false,
                        ),
                    )
                ),
                EndpointClass.ResponseClass(
                    name = "Response405Unit",
                    fields = listOf(
                        Field(
                            identifier = "status",
                            reference = Reference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(name = "String", isNullable = false),
                                        Reference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(primitive = Primitive.Any, isNullable = true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Language(Primitive.Unit)
                                    )
                                )
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        )
                    ),
                    responseAllArgsConstructor = EndpointClass.ResponseClass.ResponseAllArgsConstructor(
                        name = "Response405Unit",
                        statusCode = "405",
                        parameters = listOf(
                            Parameter(
                                identifier = "status",
                                reference = Reference.Language(
                                    primitive = Primitive.Integer
                                )
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            Reference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = Reference.Generics(
                                                    listOf(
                                                        Reference.Language(
                                                            primitive = Primitive.Any,
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
                                    isNullable = true,
                                    generics = Reference.Generics(
                                        listOf(
                                            Reference.Language(Primitive.Unit)
                                        )
                                    )
                                ),
                            )
                        ),
                    ),
                    `super` = Reference.Custom(
                        name = "Response405",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Language(
                                    Primitive.Unit
                                )
                            )
                        ),
                        isNullable = false,
                    ),
                    statusCode = "405",
                ),
            ),
            requestMapper = EndpointClass.RequestMapper(
                name = "REQUEST_MAPPER",
                conditions = listOf(
                    EndpointClass.RequestMapper.RequestCondition(
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = Reference.Custom("RequestApplicationJson"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.RequestCondition(
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = Reference.Custom("RequestApplicationXml"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.RequestCondition(
                        content = EndpointClass.Content(
                            type = "application/x-www-form-urlencoded",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = Reference.Custom("RequestApplicationXWwwFormUrlencoded"),
                        isIterable = false,
                    ),
                )
            ),
            responseMapper = EndpointClass.ResponseMapper(
                name = "RESPONSE_MAPPER",
                conditions = listOf(
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = "200",
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = Reference.Custom("Response200ApplicationXml"),
                        isIterable = false,
                    ),
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = "200",
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = Reference.Custom("Response200ApplicationJson"),
                        isIterable = false,
                    ),
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = "405",
                        content = null,
                        responseReference = Reference.Custom("Response405Unit"),
                        isIterable = false,
                    ),
                )
            )
        )

}
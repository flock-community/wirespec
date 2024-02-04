package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Reference.Language.Primitive

object ClassModelFixture {

    val endpointRequest = listOf(
        EndpointClass(
            name = "AddPetEndpoint",
            path = "/pet",
            method = "POST",
            supers = listOf(
                Reference.Custom("Wirespec.Endpoint")
            ),
            requestClasses = listOf(
                EndpointClass.RequestClass(
                    name = "RequestApplicationXml",
                    fields = listOf(
                        Field(
                            identifier = "path",
                            reference = Reference.Custom("String", false),
                            override = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Custom("Wirespec.Method", false),
                            override = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            override = true
                        ),
                    ),
                    primaryConstructor = EndpointClass.RequestClass.PrimaryConstructor(
                        name = "RequestApplicationXml",
                        parameters = listOf(
                            Parameter("path", Reference.Custom("String", false)),
                            Parameter("method", Reference.Custom("Wirespec.Method", false)),
                            Parameter(
                                "query", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "headers", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "content", Reference.Custom(
                                    "Wirespec.Content", true, Reference.Generics(
                                        listOf(
                                            Reference.Custom("Pet", false)
                                        )
                                    )
                                )
                            ),
                        ),
                    ),
                    secondaryConstructor = EndpointClass.RequestClass.SecondaryConstructor(
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
                        query = "",
                        headers = "",
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            "Request", false, Reference.Generics(
                                listOf(
                                    Reference.Custom("Pet", false)
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
                            override = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Custom("Wirespec.Method", false),
                            override = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            override = true
                        ),
                    ),
                    primaryConstructor =
                    EndpointClass.RequestClass.PrimaryConstructor(
                        name = "RequestApplicationJson",
                        parameters = listOf(
                            Parameter("path", Reference.Custom("String", false)),
                            Parameter("method", Reference.Custom("Wirespec.Method", false)),
                            Parameter(
                                "query", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "headers", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "content", Reference.Custom(
                                    "Wirespec.Content", true, Reference.Generics(
                                        listOf(
                                            Reference.Custom("Pet", false)
                                        )
                                    )
                                )
                            ),
                        ),

                        ),
                    secondaryConstructor = EndpointClass.RequestClass.SecondaryConstructor(
                        name = "RequestApplicationJson",
                        parameters = listOf(
                            Parameter("body", Reference.Custom("Pet", false)),
                        ),
                        path = EndpointClass.Path(
                            listOf(
                                EndpointClass.Path.Literal("pet")
                            )
                        ),
                        method = "POST",
                        query = "",
                        headers = "",
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            "Request", false, Reference.Generics(
                                listOf(
                                    Reference.Custom("Pet", false)
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
                            override = true
                        ),
                        Field(
                            identifier = "method",
                            reference = Reference.Custom("Wirespec.Method", false),
                            override = true
                        ),
                        Field(
                            identifier = "query",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            override = true
                        ),
                    ),
                    primaryConstructor = EndpointClass.RequestClass.PrimaryConstructor(
                        name = "RequestApplicationXWwwFormUrlencoded",
                        parameters = listOf(
                            Parameter("path", Reference.Custom("String", false)),
                            Parameter("method", Reference.Custom("Wirespec.Method", false)),
                            Parameter(
                                "query", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "headers", Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                "content", Reference.Custom(
                                    "Wirespec.Content", true, Reference.Generics(
                                        listOf(
                                            Reference.Custom("Pet", false)
                                        )
                                    )
                                )
                            ),
                        ),

                        ),
                    secondaryConstructor = EndpointClass.RequestClass.SecondaryConstructor(
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
                        query = "",
                        headers = "",
                        content = EndpointClass.Content(
                            type = "application/x-www-form-urlencoded",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        Reference.Custom(
                            "Request", false, Reference.Generics(
                                listOf(
                                    Reference.Custom("Pet", false)
                                )
                            )
                        )
                    )
                )
            ),
            responseInterfaces = listOf(
                EndpointClass.ResponseInterface(
                    Reference.Custom(
                        name = "Response2XX",
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
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "Pet",
                                            nullable = false,
                                        ),
                                    )
                                )
                            ),
                            override = true
                        )
                    ),
                    allArgsConstructor = EndpointClass.ResponseClass.AllArgsConstructor(
                        name = "Response200ApplicationXml",
                        statusCode = "200",
                        parameters = listOf(
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                identifier = "body",
                                reference = Reference.Custom(
                                    name = "Pet",
                                    nullable = false,
                                )
                            )
                        ),
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                        )
                    ),
                    returnReference = Reference.Custom(
                        name = "Response200",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        nullable = false,
                    ),
                    statusCode = "200",
                    content = EndpointClass.Content(
                        type = "application/xml",
                        reference = Reference.Custom(
                            name = "Pet",
                            nullable = false,
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
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = false,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Custom(
                                            name = "Pet",
                                            nullable = false,
                                        ),
                                    )
                                )
                            ),
                            override = true
                        )
                    ),
                    allArgsConstructor = EndpointClass.ResponseClass.AllArgsConstructor(
                        name = "Response200ApplicationJson",
                        statusCode = "200",
                        parameters = listOf(
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
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
                                identifier = "body",
                                reference = Reference.Custom(
                                    name = "Pet",
                                    nullable = false,
                                )
                            )
                        ),
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                        )
                    ),
                    returnReference = Reference.Custom(
                        name = "Response200",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        nullable = false,
                    ),
                    content = EndpointClass.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            name = "Pet",
                            nullable = false,
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
                            override = true
                        ),
                        Field(
                            identifier = "headers",
                            reference = Reference.Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Reference.Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Reference.Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            override = true
                        ),
                        Field(
                            identifier = "content",
                            reference = Reference.Custom(
                                name = "Wirespec.Content",
                                nullable = true,
                                generics = Reference.Generics(
                                    listOf(
                                        Reference.Language(Primitive.Unit)
                                    )
                                )
                            ),
                            override = true
                        )
                    ),
                    allArgsConstructor = EndpointClass.ResponseClass.AllArgsConstructor(
                        name = "Response405Unit",
                        statusCode = "405",
                        parameters = listOf(
                            Parameter(
                                identifier = "headers",
                                reference = Reference.Language(
                                    Primitive.Map, false, Reference.Generics(
                                        listOf(
                                            Reference.Custom("String", false),
                                            Reference.Language(
                                                Primitive.List, false, Reference.Generics(
                                                    listOf(
                                                        Reference.Language(Primitive.Any, true)
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                    ),
                    returnReference = Reference.Custom(
                        name = "Response405",
                        generics = Reference.Generics(
                            listOf(
                                Reference.Language(
                                    Primitive.Unit
                                )
                            )
                        ),
                        nullable = false,
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
                                nullable = false,
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
                                nullable = false,
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
                                nullable = false,
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
                                nullable = false,
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
                                nullable = false,
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
    )
}
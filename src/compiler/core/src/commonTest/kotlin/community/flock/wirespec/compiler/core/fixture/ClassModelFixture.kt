package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.nodes.Constructor
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Reference.Language.Primitive
import community.flock.wirespec.compiler.core.parse.nodes.Statement

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
                                "Wirespec.Content", true, Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            ),
                            override = true
                        ),
                    ),
                    constructors = listOf(
                        Constructor(
                            name = "RequestApplicationJson",
                            fields = listOf(
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
                            body = listOf(
                                Statement.AssignField("path", Statement.Variable("path")),
                                Statement.AssignField("method", Statement.Variable("method")),
                                Statement.AssignField("query", Statement.Variable("query")),
                                Statement.AssignField("headers", Statement.Variable("headers")),
                                Statement.AssignField("content", Statement.Variable("content"))
                            )
                        ),
                        Constructor(
                            name = "RequestApplicationJson",
                            fields = listOf(
                                Parameter("body", Reference.Custom("Pet", false)),
                            ),
                            body = listOf(
                                Statement.AssignField(
                                    "path", Statement.Concat(
                                        listOf(
                                            Statement.Literal("/"),
                                            Statement.Literal("pet"),
                                        )
                                    )
                                ),
                                Statement.AssignField("method", Statement.Variable("Wirespec.Method.POST")),
                                Statement.AssignField(
                                    "query", Statement.Initialize(
                                        Reference.Language(
                                            primitive = Primitive.Map,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(
                                                        primitive = Primitive.String
                                                    ),
                                                    Reference.Language(
                                                        primitive = Primitive.List,
                                                        generics = Reference.Generics(
                                                            listOf(
                                                                Reference.Language(
                                                                    primitive = Primitive.Any,
                                                                    nullable = true
                                                                ),
                                                            )
                                                        )
                                                    )
                                                ),
                                            )
                                        ),
                                        listOf()
                                    )
                                ),
                                Statement.AssignField(
                                    "headers", Statement.Initialize(
                                        Reference.Language(
                                            primitive = Primitive.Map,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Reference.Language(
                                                        primitive = Primitive.String
                                                    ),
                                                    Reference.Language(
                                                        primitive = Primitive.List,
                                                        generics = Reference.Generics(
                                                            listOf(
                                                                Reference.Language(
                                                                    primitive = Primitive.Any,
                                                                    nullable = true
                                                                ),
                                                            )
                                                        )
                                                    )
                                                ),
                                            )
                                        ),
                                        listOf()
                                    )
                                ),
                                Statement.AssignField(
                                    "content",
                                    Statement.Initialize(
                                        Reference.Custom("Wirespec.Content"),
                                        listOf("\"application/json\"", "body")
                                    )
                                )
                            )
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
                            identifier = "body",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                            override = false
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
                    )
                ),
                EndpointClass.ResponseClass(
                    name = "Response200ApplicationJson",
                    fields = listOf(
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
                            identifier = "body",
                            reference = Reference.Custom(
                                name = "Pet",
                                nullable = false,
                            ),
                            override = false
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
                        )
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
                    EndpointClass.RequestMapper.Condition(
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
                    EndpointClass.RequestMapper.Condition(
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
                    EndpointClass.RequestMapper.Condition(
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
                    EndpointClass.ResponseMapper.Condition(
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
                    EndpointClass.ResponseMapper.Condition(
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
                    EndpointClass.ResponseMapper.Condition(
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
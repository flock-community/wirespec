package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.nodes.Constructor
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Reference.Language
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
                        Field("path", Reference.Custom("String", false)),
                        Field("method", Reference.Custom("Wirespec.Method", false)),
                        Field(
                            "query", Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        Field(
                            "headers", Language(
                                Primitive.Map, false, Reference.Generics(
                                    listOf(
                                        Reference.Custom("String", false),
                                        Language(
                                            Primitive.List, false, Reference.Generics(
                                                listOf(
                                                    Language(Primitive.Any, true)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        Field(
                            "content", Reference.Custom(
                                "Wirespec.Content", true, Reference.Generics(
                                    listOf(
                                        Reference.Custom("Pet", false)
                                    )
                                )
                            )
                        ),
                    ),
                    constructors = listOf(
                        Constructor(
                            name = "RequestApplicationJson",
                            fields = listOf(
                                Parameter("path", Reference.Custom("String", false)),
                                Parameter("method", Reference.Custom("Wirespec.Method", false)),
                                Parameter(
                                    "query", Language(
                                        Primitive.Map, false, Reference.Generics(
                                            listOf(
                                                Reference.Custom("String", false),
                                                Language(
                                                    Primitive.List, false, Reference.Generics(
                                                        listOf(
                                                            Language(Primitive.Any, true)
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ),
                                Parameter(
                                    "headers", Language(
                                        Primitive.Map, false, Reference.Generics(
                                            listOf(
                                                Reference.Custom("String", false),
                                                Language(
                                                    Primitive.List, false, Reference.Generics(
                                                        listOf(
                                                            Language(Primitive.Any, true)
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
                                        Language(
                                            primitive = Primitive.Map,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Language(
                                                        primitive = Primitive.String
                                                    ),
                                                    Language(
                                                        primitive = Primitive.List,
                                                        generics = Reference.Generics(
                                                            listOf(
                                                                Language(
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
                                        Language(
                                            primitive = Primitive.Map,
                                            generics = Reference.Generics(
                                                listOf(
                                                    Language(
                                                        primitive = Primitive.String
                                                    ),
                                                    Language(
                                                        primitive = Primitive.List,
                                                        generics = Reference.Generics(
                                                            listOf(
                                                                Language(
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
            requestMapper = EndpointClass.RequestMapper(
                name = "REQUEST_MAPPER",
                conditions = listOf(
                    EndpointClass.RequestMapper.Condition(
                        contentType = "application/json",
                        contentReference = Reference.Custom("Pet"),
                        responseReference = Reference.Custom("RequestApplicationJson"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.Condition(
                        contentType = "application/xml",
                        contentReference = Reference.Custom("Pet"),
                        responseReference = Reference.Custom("RequestApplicationXml"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.Condition(
                        contentType = "application/x-www-form-urlencoded",
                        contentReference = Reference.Custom("Pet"),
                        responseReference = Reference.Custom("RequestApplicationXWwwFormUrlencoded"),
                        isIterable = false,
                    ),
                )
            )
        )
    )
}
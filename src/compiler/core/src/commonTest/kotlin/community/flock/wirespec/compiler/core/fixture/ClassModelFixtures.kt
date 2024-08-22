package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.emit.transformer.ClassReference
import community.flock.wirespec.compiler.core.emit.transformer.ClassReference.Language.Primitive
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.FieldClass
import community.flock.wirespec.compiler.core.emit.transformer.Parameter

object ClassModelFixtures {
    val endpointClass =
        EndpointClass(
            name = "AddPetEndpoint",
            functionName = "addPet",
            path = "/pet",
            method = "POST",
            supers = listOf(
                ClassReference.Wirespec("Endpoint")
            ),
            requestClasses = listOf(
                EndpointClass.RequestClass(
                    name = "RequestApplicationXml",
                    fields = listOf(
                        FieldClass(
                            identifier = "path",
                            reference = ClassReference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "method",
                            reference = ClassReference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "query",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(
                                            name = "String",
                                            isNullable = false
                                        ),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(
                                            name = "String",
                                            isNullable = false
                                        ),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(Primitive.Any, true)
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
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom("Pet", false)
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
                                reference = ClassReference.Custom(
                                    name = "String",
                                    isNullable = false,
                                    isOptional = false,
                                ),
                            ),
                            Parameter(
                                identifier = "method",
                                reference = ClassReference.Wirespec(
                                    name = "Method",
                                    isNullable = false,
                                    isOptional = false,
                                ),
                            ),
                            Parameter(
                                identifier = "query",
                                reference = ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    isOptional = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                "headers", ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    isOptional = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom("String", false),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                "content", ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    isOptional = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
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
                            Parameter("body", ClassReference.Custom("Pet", false)),
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        ClassReference.Custom(
                            name = "Request",
                            isNullable = false,
                            isInternal = true,
                            generics = ClassReference.Generics(
                                listOf(
                                    ClassReference.Custom(
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
                        FieldClass(
                            identifier = "path",
                            reference = ClassReference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "method",
                            reference = ClassReference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "query",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map,
                                isNullable = false,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(
                                            name = "String", isNullable = false
                                        ),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom("Pet", false)
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
                            Parameter("path", ClassReference.Custom("String", false)),
                            Parameter("method", ClassReference.Wirespec("Method", false)),
                            Parameter(
                                "query", ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                "headers", ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String", false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                "content", ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
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
                                "body", ClassReference.Custom(
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        ClassReference.Custom(
                            name = "Request",
                            isNullable = false,
                            isInternal = true,
                            generics = ClassReference.Generics(
                                listOf(
                                    ClassReference.Custom(
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
                        FieldClass(
                            identifier = "path",
                            reference = ClassReference.Custom("String", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "method",
                            reference = ClassReference.Wirespec("Method", false),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "query",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom("Pet", false)
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
                            Parameter("path", ClassReference.Custom("String", false)),
                            Parameter("method", ClassReference.Wirespec("Method", false)),
                            Parameter(
                                identifier = "query", ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                "headers", ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(Primitive.Any, true)
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            Parameter(
                                "content", ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
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
                            Parameter("body", ClassReference.Custom("Pet", false)),
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    supers = listOf(
                        ClassReference.Custom(
                            name = "Request",
                            isNullable = false,
                            isInternal = true,
                            generics = ClassReference.Generics(
                                listOf(
                                    ClassReference.Custom(
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
                    name = ClassReference.Custom(
                        name = "Response2XX",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    ),
                    `super` = ClassReference.Custom(
                        name = "Response",
                        isInternal = true,
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    )
                ),
                EndpointClass.ResponseInterface(
                    ClassReference.Custom(
                        name = "Response4XX",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    ),
                    ClassReference.Custom(
                        name = "Response",
                        isInternal = true,
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    )
                ),
                EndpointClass.ResponseInterface(
                    ClassReference.Custom(
                        name = "Response200",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    ),
                    ClassReference.Custom(
                        name = "Response2XX",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    )
                ),
                EndpointClass.ResponseInterface(
                    ClassReference.Custom(
                        name = "Response405",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    ),
                    ClassReference.Custom(
                        name = "Response4XX",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom("T")
                            )
                        )
                    )
                ),
            ),
            responseClasses = listOf(
                EndpointClass.ResponseClass(
                    name = "Response200ApplicationXml",
                    fields = listOf(
                        FieldClass(
                            identifier = "status",
                            reference = ClassReference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                            isPrivate = true,
                            isFinal = true
                        ),
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(
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
                                reference = ClassReference.Language(
                                    primitive = Primitive.Integer
                                )
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                reference = ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    responseParameterConstructor = EndpointClass.ResponseClass.ResponseParameterConstructor(
                        name = "Response200ApplicationXml",
                        statusCode = "200",
                        parameters = emptyList(),
                        headers = listOf(),
                    ),
                    `super` = ClassReference.Custom(
                        name = "Response200",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        isNullable = false,
                    ),
                    statusCode = "200",
                    content = EndpointClass.Content(
                        type = "application/xml",
                        reference = ClassReference.Custom(
                            name = "Pet",
                            isNullable = false,
                        ),
                    ),
                ),
                EndpointClass.ResponseClass(
                    name = "Response200ApplicationJson",
                    statusCode = "200",
                    fields = listOf(
                        FieldClass(
                            identifier = "status",
                            reference = ClassReference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isPrivate = true,
                            isFinal = true
                        ),
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                            isPrivate = true,
                            isFinal = true
                        ),
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = false,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(
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
                                reference = ClassReference.Language(
                                    primitive = Primitive.Integer
                                ),
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                reference = ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        )
                    ),
                    responseParameterConstructor = EndpointClass.ResponseClass.ResponseParameterConstructor(
                        name = "Response200ApplicationJson",
                        statusCode = "200",
                        parameters = emptyList(),
                        headers = listOf(),
                    ),
                    `super` = ClassReference.Custom(
                        name = "Response200",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Custom(
                                    name = "Pet",
                                )
                            )
                        ),
                        isNullable = false,
                    ),
                    content = EndpointClass.Content(
                        type = "application/json",
                        reference = ClassReference.Custom(
                            name = "Pet",
                            isNullable = false,
                        ),
                    )
                ),
                EndpointClass.ResponseClass(
                    name = "Response405Unit",
                    fields = listOf(
                        FieldClass(
                            identifier = "status",
                            reference = ClassReference.Language(
                                primitive = Primitive.Integer
                            ),
                            isOverride = true,
                            isFinal = true,
                            isPrivate = true
                        ),
                        FieldClass(
                            identifier = "headers",
                            reference = ClassReference.Language(
                                primitive = Primitive.Map, isNullable = false, generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Custom(name = "String", isNullable = false),
                                        ClassReference.Language(
                                            primitive = Primitive.List,
                                            isNullable = false,
                                            generics = ClassReference.Generics(
                                                listOf(
                                                    ClassReference.Language(
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
                        FieldClass(
                            identifier = "content",
                            reference = ClassReference.Wirespec(
                                name = "Content",
                                isNullable = true,
                                generics = ClassReference.Generics(
                                    listOf(
                                        ClassReference.Language(Primitive.Unit)
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
                                reference = ClassReference.Language(
                                    primitive = Primitive.Integer
                                )
                            ),
                            Parameter(
                                identifier = "headers",
                                reference = ClassReference.Language(
                                    primitive = Primitive.Map,
                                    isNullable = false,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Custom(
                                                name = "String",
                                                isNullable = false
                                            ),
                                            ClassReference.Language(
                                                primitive = Primitive.List,
                                                isNullable = false,
                                                generics = ClassReference.Generics(
                                                    listOf(
                                                        ClassReference.Language(
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
                                reference = ClassReference.Wirespec(
                                    name = "Content",
                                    isNullable = true,
                                    generics = ClassReference.Generics(
                                        listOf(
                                            ClassReference.Language(Primitive.Unit)
                                        )
                                    )
                                ),
                            )
                        ),
                    ),
                    responseParameterConstructor = EndpointClass.ResponseClass.ResponseParameterConstructor(
                        name = "Response405Unit",
                        statusCode = "405",
                        parameters = emptyList(),
                        headers = listOf(),
                    ),
                    `super` = ClassReference.Custom(
                        name = "Response405",
                        generics = ClassReference.Generics(
                            listOf(
                                ClassReference.Language(
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = ClassReference.Custom("RequestApplicationJson"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.RequestCondition(
                        content = EndpointClass.Content(
                            type = "application/xml",
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = ClassReference.Custom("RequestApplicationXml"),
                        isIterable = false,
                    ),
                    EndpointClass.RequestMapper.RequestCondition(
                        content = EndpointClass.Content(
                            type = "application/x-www-form-urlencoded",
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = ClassReference.Custom("RequestApplicationXWwwFormUrlencoded"),
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
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = ClassReference.Custom("Response200ApplicationXml"),
                        isIterable = false,
                    ),
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = "200",
                        content = EndpointClass.Content(
                            type = "application/json",
                            reference = ClassReference.Custom(
                                name = "Pet",
                                isNullable = false,
                            ),
                        ),
                        responseReference = ClassReference.Custom("Response200ApplicationJson"),
                        isIterable = false,
                    ),
                    EndpointClass.ResponseMapper.ResponseCondition(
                        statusCode = "405",
                        content = null,
                        responseReference = ClassReference.Custom("Response405Unit"),
                        isIterable = false,
                    ),
                )
            )
        )
}

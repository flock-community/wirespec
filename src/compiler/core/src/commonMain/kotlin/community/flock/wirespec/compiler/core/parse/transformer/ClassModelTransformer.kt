package community.flock.wirespec.compiler.core.parse.transformer

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.nodes.ClassModel
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type

object ClassModelTransformer {

    fun transform(ast: List<Definition>): List<ClassModel> {
        return ast.map {
            when (it) {
                is Endpoint -> it.transform()
                is Enum -> TODO()
                is Refined -> TODO()
                is Type -> TODO()
            }
        }
    }

    private fun Endpoint.transform(): EndpointClass {
        return EndpointClass(
            name = "${name}Endpoint",
            path = path.joinToString("/") { it.emit() },
            method = method.name,
            requestClasses = requests.map {
                EndpointClass.RequestClass(
                    name = "Request${it.content?.emit() ?: "Unit"}",
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
                                            "List", false, Reference.Generics(
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
                                            "List", false, Reference.Generics(
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
                                nullable = true,
                                generics = Reference.Generics(it.content
                                    ?.let { listOf(Reference.Custom(it.type, false)) }
                                    ?: listOf(Reference.Language(Reference.Language.Primitive.Unit, false))
                                )
                            )
                        ),
                    ),
                    primaryConstructor = EndpointClass.RequestClass.PrimaryConstructor(
                        name = "PrimaryConstructor",
                        parameters = listOf()
                    ),
                    secondaryConstructor = EndpointClass.RequestClass.SecondaryConstructor(
                        name = "SecondaryConstructor",
                        parameters = listOf(),
                        path = EndpointClass.Path(listOf(
                            EndpointClass.Path.Literal("pet")
                        )),
                        method = "POST",
                        query = "",
                        headers = "",
                    ),
                    supers = emptyList()
                )
            },
            requestMapper = EndpointClass.RequestMapper(
                name = "REQUEST_MAPPER",
                conditions = emptyList()
            ),
            responseMapper = EndpointClass.ResponseMapper(
                name = "RESPONSE_MAPPER",
                conditions = emptyList()
            ),
            responseInterfaces = listOf(),
            responseClasses = listOf(),
            supers = emptyList()
        )
    }

    private fun Endpoint.Segment.emit(): String =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${${identifier.emit()}}"
        }

    private fun Type.Shape.Field.Identifier.emit() =
        value
            .split("-", ".")
            .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
            .joinToString("")

    fun Endpoint.Content.emit() = type
        .substringBefore(";")
        .split("/", "-")
        .joinToString("") { it.firstToUpper() }
        .replace("+", "")

    fun Type.Shape.Field.Reference.emit() =
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


}
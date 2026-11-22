package community.flock.wirespec.emitters.php

import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.TypeParameter
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.function
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.Function as LanguageFunction
import community.flock.wirespec.compiler.core.ir.Type as LanguageType
import community.flock.wirespec.compiler.core.ir.Union as LanguageUnion

private val value = Name.of("value")

/** A refined type validates and prints its own `$this->value`, and PHP spells `toString` as `__toString`. */
internal fun File.withPhpRefinedFunctions(refined: Refined): File = transform {
    matchingElements { struct: Struct ->
        struct.copy(
            elements = struct.elements.map { element ->
                when {
                    element !is LanguageFunction -> element
                    element.name == Name.of("validate") -> function("validate") {
                        returnType(LanguageType.Boolean)
                        returns(refined.reference.convertConstraint(FieldCall(field = value)))
                    }
                    element.name == Name.of("toString") -> function(Name(listOf("__toString"))) {
                        returnType(LanguageType.String)
                        returns(
                            when (refined.reference.type) {
                                is Reference.Primitive.Type.String -> FieldCall(field = value)
                                else -> RawExpression("(string) \$this->value")
                            },
                        )
                    }
                    else -> element
                }
            },
        )
    }
}

/** A generated enum is a backed enum, which PHP builds from its label with `from`. */
internal fun File.withEnumFrom(enum: Enum): File = transform {
    expression { expression, transformer ->
        when {
            expression is FunctionCall &&
                expression.name == Name.of("valueOf") &&
                (expression.receiver as? RawExpression)?.code == enum.identifier.value ->
                expression.copy(
                    receiver = RawExpression(Name.of(enum.identifier.value).pascalCase()),
                    name = Name.of("from"),
                ).transformChildren(transformer)
            else -> expression.transformChildren(transformer)
        }
    }
}

/** An enum case is an identifier, and `class` is the one name a case cannot have. */
internal fun String.sanitizeEnumEntry(): String = map { if (it.isLetterOrDigit() || it == '_') it else '_' }
    .joinToString("")
    .let { if (it.firstOrNull()?.isDigit() != false || it.equals("class", ignoreCase = true)) "_$it" else it }

private val response = Name.of("Response")

private val result = LanguageType.Custom("R")

/** The class of every response of the endpoint, such as `Response200`, with the type of its body, in the order of the spec. */
private fun Endpoint.responseClasses(): List<Pair<String, LanguageType>> = responses
    .distinctBy { it.status }
    .map { "Response${it.status.replaceFirstChar(Char::uppercaseChar)}" to (it.content?.reference?.convert() ?: LanguageType.Unit) }

/**
 * PHP has no sealed types, so an endpoint's `Response` interface also declares `fold`: one required closure per
 * response, each called with the body and the response itself. Leaving out a response is an error, so `fold`
 * handles the responses exhaustively, like a `when` over a sealed type.
 */
internal fun File.withResponseFold(endpoint: Endpoint): File = endpoint.responseClasses().let { responses ->
    transform {
        matchingElements { union: LanguageUnion ->
            if (union.name == response) union.withFold(responses) else union
        }
        matchingElements { struct: Struct ->
            responses.find { (name, _) -> name == struct.name.pascalCase() }
                ?.let { (name, _) -> struct.copy(elements = struct.elements + responses.fold(body = listOf(ReturnStatement(name.callOn())))) }
                ?: struct
        }
    }
}

/** The `Response` union becomes an interface, because a union has no methods to declare `fold` on. */
private fun LanguageUnion.withFold(responses: List<Pair<String, LanguageType>>): Interface = Interface(
    name = name,
    elements = listOf(responses.fold(body = emptyList())),
    extends = listOfNotNull(extends),
    typeParameters = typeParameters,
)

private fun List<Pair<String, LanguageType>>.fold(body: List<Statement>): LanguageFunction = LanguageFunction(
    name = Name.of("fold"),
    typeParameters = listOf(TypeParameter(result)),
    parameters = map { (name, bodyType) ->
        Parameter(Name(listOf(name.onParameter())), LanguageType.Function(listOf(bodyType, LanguageType.Custom(Name(listOf(name)))), result))
    },
    returnType = result,
    body = body,
)

/** The IR has no call of a closure held in a variable, so the call `$on200($this->body, $this)` is written as PHP. */
private fun String.callOn(): RawExpression = RawExpression("\$${onParameter()}(\$this->body, \$this)")

/** The `fold` parameter for a response class: `on200` for `Response200`. */
private fun String.onParameter(): String = "on${removePrefix(response.value())}"

/**
 * A call returns the union of the endpoint's response classes, such as `Response200|Response404`, instead of its
 * `Response` interface: the signature lists every response, and a static analyser narrows each `instanceof` branch.
 * [qualified] writes them as `PutTodo.Response200`, for a client outside the endpoint's file.
 */
internal fun <T : Element> T.withResponseUnions(endpoints: List<Endpoint>, qualified: Boolean): T = endpoints
    .associate { endpoint ->
        val prefix = if (qualified) "${endpoint.irName()}." else ""
        "$prefix${response.value()}" to endpoint.responseClasses().joinToString("|") { (name, _) -> "$prefix$name" }
    }
    .let { unions ->
        transform {
            matching { type: LanguageType.Custom ->
                unions[type.name.value()]
                    ?.takeIf { type.generics == listOf(LanguageType.Wildcard) }
                    ?.let { LanguageType.Custom(Name(listOf(it))) }
                    ?: type
            }
        }
    }

/** The name the IR converter gives the endpoint in the types of its clients, such as `PutTodo` in `PutTodo.Response`. */
private fun Endpoint.irName(): String = Name.of(identifier.value).parts.filter { part -> part.any(Char::isLetterOrDigit) }.joinToString("")

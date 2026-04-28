package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.Case
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.ErrorStatement
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import community.flock.wirespec.ir.generator.TypeScriptGenerator
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Type as LanguageType

internal fun <T : Element> T.renameValidateAndBindObjReceiver(
    typeName: String,
    fieldNames: Set<String>,
): T = transform {
    matchingElements { fn: LanguageFunction ->
        if (fn.name == Name.of("validate")) {
            fn.copy(
                name = Name.of("validate$typeName"),
                parameters = listOf(Parameter(Name.of("obj"), LanguageType.Custom(typeName))),
            ).transform {
                statementAndExpression { s, t ->
                    when {
                        s is FunctionCall && s.name == Name.of("validate") && s.receiver != null && s.typeArguments.isNotEmpty() -> {
                            val tn = (s.typeArguments.first() as? LanguageType.Custom)?.name ?: ""
                            FunctionCall(name = Name.of("validate$tn"), arguments = mapOf(Name.of("obj") to t.transformExpression(s.receiver!!)))
                        }
                        s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames ->
                            FieldCall(receiver = VariableReference(Name.of("obj")), field = s.field)
                        else -> s.transformChildren(t)
                    }
                }
            }
        } else {
            fn
        }
    }
}

internal fun <T : Element> T.stripTrailingSpaceFromErrorMessage(): T = transform {
    statement { stmt, transformer ->
        when (stmt) {
            is Switch -> stmt.copy(
                default = stmt.default?.map { s ->
                    if (s is ErrorStatement && s.message is BinaryOp) {
                        val binary = s.message as BinaryOp
                        val literal = binary.left as? Literal
                        if (literal != null) {
                            ErrorStatement(Literal(literal.value.toString().trimEnd(' '), literal.type))
                        } else {
                            s
                        }
                    } else {
                        s
                    }
                },
            ).transformChildren(transformer)
            else -> stmt.transformChildren(transformer)
        }
    }
}

internal fun <T : Element> T.bindCallToRequestParams(): T = transform {
    matchingElements { iface: Interface ->
        if (iface.name == Name.of("Call")) {
            iface.copy(
                elements = iface.elements.map { element ->
                    if (element is LanguageFunction) {
                        element.copy(
                            parameters = listOf(
                                Parameter(Name.of("params"), LanguageType.Custom("RequestParams")),
                            ),
                        )
                    } else {
                        element
                    }
                },
            )
        } else {
            iface
        }
    }
}

internal fun transformPatternSwitchToValueSwitch(): Transformer = transformer {
    statement { stmt, tr ->
        if (stmt is Switch && stmt.cases.any { it.type != null }) {
            val varName = stmt.variable?.camelCase() ?: "r"
            val transformedCases = stmt.cases.map { case ->
                val typeName = (case.type as? LanguageType.Custom)?.name
                val statusNum = typeName
                    ?.substringAfterLast(".")
                    ?.removePrefix("Response")
                    ?.toIntOrNull()
                if (statusNum != null && typeName != null) {
                    val exprCode = TypeScriptGenerator.generateExpression(tr.transformExpression(stmt.expression))
                    val castAssignment = Assignment(
                        name = Name.of(varName),
                        value = RawExpression("$exprCode as $typeName"),
                        isProperty = false,
                    )
                    Case(
                        value = Literal(statusNum, LanguageType.Integer()),
                        body = listOf(castAssignment) + case.body.map { tr.transformStatement(it) },
                        type = null,
                    )
                } else {
                    case.copy(body = case.body.map { tr.transformStatement(it) })
                }
            }
            Switch(
                expression = FieldCall(
                    receiver = tr.transformExpression(stmt.expression),
                    field = Name.of("status"),
                ),
                cases = transformedCases,
                default = stmt.default?.map { tr.transformStatement(it) },
                variable = null,
            )
        } else {
            stmt.transformChildren(tr)
        }
    }
}

internal fun buildApiConst(endpoint: Endpoint): RawElement {
    val apiName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
    val method = endpoint.method.name
    val pathString = endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return raw(
        """
        |export const client:Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |  from: (it) => fromRawResponse(serialization, it),
        |  to: (it) => toRawRequest(serialization, it)
        |})
        |export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |  from: (it) => fromRawRequest(serialization, it),
        |  to: (it) => toRawResponse(serialization, it)
        |})
        |export const api = {
        |  name: "$apiName",
        |  method: "$method",
        |  path: "$pathString",
        |  server,
        |  client
        |} as const
        """.trimMargin(),
    )
}

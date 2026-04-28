package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.Union as LanguageUnion

internal fun File.replaceRefinedFunctions(refined: Refined): File = transform {
    matchingElements { struct: Struct ->
        struct.copy(
            elements = struct.elements.mapNotNull { element ->
                when {
                    element is LanguageFunction && element.name == Name.of("validate") -> {
                        val constraintExpr = refined.reference.convertConstraint(
                            FieldCall(VariableReference(Name.of("self")), Name.of("value"))
                        )
                        function("validate") {
                            arg("self", LanguageType.Custom(""))
                            returnType(LanguageType.Boolean)
                            returns(constraintExpr)
                        }
                    }
                    element is LanguageFunction && element.name == Name.of("toString") -> {
                        val toStringExpr = when (refined.reference.type) {
                            is Reference.Primitive.Type.String -> "self.value"
                            else -> "str(self.value)"
                        }
                        function("__str__") {
                            arg("self", LanguageType.Custom(""))
                            returnType(LanguageType.String)
                            returns(RawExpression(toStringExpr))
                        }
                    }
                    else -> element
                }
            },
        )
    }
}

internal fun File.splitEndpointStructsToModuleLevel(): File {
    val namespace = findElement<Namespace>()!!
    val flattened = namespace.flattenNestedStructs()
    val (moduleElements, classElements) = flattened.elements.partition { it is Struct || it is LanguageUnion }
    val endpointClass = Namespace(
        name = namespace.name,
        elements = classElements,
        extends = namespace.extends,
    )
    return LanguageFile(namespace.name, moduleElements + endpointClass)
}

internal fun <T : Element> T.snakeCaseHandlerAndCallMethods(): T = transform {
    matchingElements { iface: Interface ->
        if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
            iface.copy(
                elements = iface.elements.map { element ->
                    if (element is LanguageFunction) {
                        element.copy(name = Name.of(element.name.snakeCase()))
                    } else element
                },
            )
        } else iface
    }
}

internal fun <T : Element> T.flattenEndpointTypeRefs(endpointName: String): T = transform {
    type { type, _ ->
        if (type is LanguageType.Custom && type.name.startsWith("$endpointName.")) {
            val suffix = type.name.removePrefix("$endpointName.")
            if (suffix == "Call" || suffix == "Handler") type
            else type.copy(name = suffix)
        } else type
    }
}

internal fun <T : Element> T.addSelfReceiverToClientFields(): T {
    val struct = (this as? File)?.findElement<Struct>()
    val fieldNames = struct?.fields?.map { it.name.value() }?.toSet() ?: emptySet()
    if (fieldNames.isEmpty()) return this

    return transform {
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is FieldCall -> {
                    if (stmt.receiver == null && stmt.field.value() in fieldNames) {
                        FieldCall(receiver = VariableReference(Name.of("self")), field = stmt.field)
                    } else {
                        FieldCall(
                            receiver = stmt.receiver?.let { tr.transformExpression(it) },
                            field = stmt.field,
                        )
                    }
                }
                else -> stmt.transformChildren(tr)
            }
        }
    }
}

internal fun <T : Element> T.snakeCaseClientFunctions(): T = transform {
    matchingElements { func: LanguageFunction ->
        func.copy(
            name = Name.of(func.name.snakeCase()),
            parameters = listOf(Parameter(Name.of("self"), LanguageType.Custom(""))) + func.parameters,
        )
    }
    statementAndExpression { stmt, tr ->
        when (stmt) {
            is FunctionCall -> {
                val nameStr = stmt.name.value()
                val newName = if ("." in nameStr) stmt.name else Name.of(Name.of(nameStr).snakeCase())
                FunctionCall(
                    name = newName,
                    receiver = stmt.receiver?.let { tr.transformExpression(it) },
                    arguments = stmt.arguments.mapValues { (_, v) -> tr.transformExpression(v) },
                    isAwait = stmt.receiver != null,
                )
            }
            else -> stmt.transformChildren(tr)
        }
    }
}

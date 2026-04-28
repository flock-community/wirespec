package community.flock.wirespec.ir.transformer

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Type as LanguageType

fun Struct.markMembersAsOverride(): Struct = copy(
    fields = fields.map { f -> f.copy(isOverride = true) },
    elements = elements.map { element ->
        if (element is Function) element.copy(isOverride = true) else element
    },
)

fun <E : Element> E.ensureEmptyStructHasConstructor(): E = transform {
    matchingElements { struct: Struct ->
        if (struct.fields.isEmpty()) struct.copy(constructors = listOf(Constructor(emptyList(), emptyList())))
        else struct
    }
}

fun <E : Element> E.injectEnumLabelField(
    sanitizeEntry: (String) -> String,
    extraElements: (LanguageEnum) -> List<Element> = { emptyList() },
): E = transform {
    matchingElements { languageEnum: LanguageEnum ->
        val withLabel = languageEnum.withLabelField(sanitizeEntry)
        val extras = extraElements(withLabel)
        if (extras.isEmpty()) withLabel
        else withLabel.copy(elements = withLabel.elements + extras)
    }
}

fun <E : Element> E.sanitizeEnumEntries(
    sanitizeEntry: (String) -> String,
): E = transform {
    matchingElements { languageEnum: LanguageEnum ->
        languageEnum.copy(
            entries = languageEnum.entries.map {
                it.copy(name = Name.of(sanitizeEntry(it.name.value())))
            },
        )
    }
}

fun <E : Element> E.injectSelfReceiverToValidate(
    fieldNames: Set<String>,
    selfParamName: String = "self",
): E = transform {
    matchingElements { fn: Function ->
        if (fn.name == Name.of("validate")) {
            fn.copy(
                parameters = listOf(Parameter(Name.of(selfParamName), LanguageType.Custom(""))),
            ).transform {
                statementAndExpression { s, t ->
                    if (s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames) {
                        FieldCall(receiver = VariableReference(Name.of("self")), field = s.field)
                    } else s.transformChildren(t)
                }
            }
        } else fn
    }
}

fun Definition.sortKey(): Int = when (this) {
    is Enum -> 1
    is Refined -> 2
    is Type -> 3
    is Union -> 4
    is Endpoint -> 5
    is Channel -> 6
}

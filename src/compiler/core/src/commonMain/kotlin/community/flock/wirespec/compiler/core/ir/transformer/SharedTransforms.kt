package community.flock.wirespec.compiler.core.ir.transformer

import community.flock.wirespec.compiler.core.ir.Assignment
import community.flock.wirespec.compiler.core.ir.Constructor
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.EnumReference
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.withLabelField
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.ir.Enum as LanguageEnum
import community.flock.wirespec.compiler.core.ir.Type as LanguageType

public fun Struct.markMembersAsOverride(): Struct = copy(
    fields = fields.map { f -> if (f is Field) f.copy(isOverride = true) else f },
    elements = elements.map { element ->
        if (element is Function) element.copy(isOverride = true) else element
    },
)

public fun <E : Element> E.ensureEmptyStructHasConstructor(): E = transform {
    matchingElements { struct: Struct ->
        if (struct.fields.isEmpty()) {
            struct.copy(constructors = listOf(Constructor(emptyList(), emptyList(), visibility = null)))
        } else {
            struct
        }
    }
}

/**
 * A string literal type admits one value, so the field takes that value as its default. For a language whose fields
 * can have one.
 */
public fun <E : Element> E.withLiteralDefaults(): E = transform {
    matchingElements { struct: Struct -> struct.copy(fields = struct.fields.map { (it as? Field)?.withLiteralDefault() ?: it }) }
}

private fun Field.withLiteralDefault(): Field = when (val type = type) {
    is LanguageType.StringLiteral -> copy(initializer = Literal(type.value, LanguageType.String))
    else -> this
}

/**
 * A string literal type admits one value, so a struct with such a field gets a constructor that takes the other
 * fields and fills that value in. For a language whose fields have no default.
 */
public fun <E : Element> E.withLiteralConstructor(): E = transform {
    matchingElements { struct: Struct -> struct.literalConstructor()?.let { struct.copy(constructors = struct.constructors + it) } ?: struct }
}

private fun Struct.literalConstructor(): Constructor? = fieldList()
    .takeIf { fields -> fields.any { it.type is LanguageType.StringLiteral } }
    ?.let { fields ->
        Constructor(
            parameters = fields.filterNot { it.type is LanguageType.StringLiteral }.map { Parameter(it.name, it.type) },
            body = fields.map { Assignment(it.name, it.literalOrParameter(), isProperty = true) },
            visibility = null,
        )
    }

private fun Field.literalOrParameter(): Expression = when (val type = type) {
    is LanguageType.StringLiteral -> Literal(type.value, LanguageType.String)
    else -> VariableReference(name)
}

public fun <E : Element> E.injectEnumLabelField(
    sanitizeEntry: (String) -> String,
    extraElements: (LanguageEnum) -> List<Element> = { emptyList() },
): E = transform {
    matchingElements { languageEnum: LanguageEnum ->
        val withLabel = languageEnum.withLabelField(sanitizeEntry)
        val extras = extraElements(withLabel)
        if (extras.isEmpty()) {
            withLabel
        } else {
            withLabel.copy(elements = withLabel.elements + extras)
        }
    }
}

public fun <E : Element> E.sanitizeEnumEntries(
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

/**
 * Renames the entry of every enum default the way [sanitizeEntry] renames the entries of the enum
 * itself, so the default refers to the entry as the language spells it.
 */
public fun <E : Element> E.sanitizeEnumDefaults(
    sanitizeEntry: (String) -> String,
): E = transform {
    fields { field ->
        when (val initializer = field.initializer) {
            is EnumReference -> field.copy(initializer = initializer.copy(entry = Name(listOf(sanitizeEntry(initializer.entry.value())))))
            else -> field
        }
    }
}

public fun Definition.sortKey(): Int = when (this) {
    is Enum -> 1
    is Refined -> 2
    is Type -> 3
    is Union -> 4
    is Endpoint -> 5
    is Channel -> 6
    is Rpc -> 7
    is Part -> 8
}

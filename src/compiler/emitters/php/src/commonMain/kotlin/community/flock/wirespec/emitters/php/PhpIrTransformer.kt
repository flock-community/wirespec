package community.flock.wirespec.emitters.php

import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.function
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.Function as LanguageFunction
import community.flock.wirespec.compiler.core.ir.Type as LanguageType

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
            expression is FunctionCall && expression.name == Name.of("valueOf") &&
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

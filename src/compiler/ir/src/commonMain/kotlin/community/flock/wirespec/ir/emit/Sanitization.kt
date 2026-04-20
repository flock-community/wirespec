package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.Function as LanguageFunction

data class SanitizationConfig(
    val reservedKeywords: Set<String>,
    val escapeKeyword: (String) -> String,
    val fieldNameCase: (Name) -> Name,
    val parameterNameCase: (Name) -> Name,
    val sanitizeSymbol: (String) -> String,
    val extraStatementTransforms: ((Statement, Transformer) -> Statement)? = null,
    val escapeFieldKeywords: Boolean = true,
    val functionNameCase: (Name) -> Name = { it },
)

fun <T : Element> T.sanitizeNames(config: SanitizationConfig): T = transform {
    fields { field ->
        field.copy(name = config.sanitizeFieldName(field.name))
    }
    parameters { param ->
        val casedName = config.parameterNameCase(param.name)
        val sanitized = config.sanitizeSymbol(casedName.value())
        val escaped = if (sanitized in config.reservedKeywords) config.escapeKeyword(sanitized) else sanitized
        param.copy(name = Name(listOf(escaped)))
    }
    matchingElements { fn: LanguageFunction ->
        fn.copy(name = config.functionNameCase(fn.name))
    }
    statementAndExpression { stmt, tr ->
        val extra = config.extraStatementTransforms
        val renamed = if (stmt is FunctionCall) stmt.copy(name = config.functionNameCase(stmt.name)) else stmt
        when {
            renamed is FieldCall -> FieldCall(
                receiver = renamed.receiver?.let { tr.transformExpression(it) },
                field = config.sanitizeFieldName(renamed.field),
            )
            extra != null -> extra(renamed, tr)
            else -> renamed.transformChildren(tr)
        }
    }
}

fun SanitizationConfig.sanitizeFieldName(name: Name): Name {
    val cased = fieldNameCase(name)
    val sanitized = sanitizeSymbol(cased.value())
    val escaped = if (escapeFieldKeywords && sanitized in reservedKeywords) escapeKeyword(sanitized) else sanitized
    return Name(listOf(escaped))
}

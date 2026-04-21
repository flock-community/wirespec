package community.flock.wirespec.ir.transformer

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren

data class SanitizationConfig(
    val reservedKeywords: Set<String>,
    val escapeKeyword: (String) -> String,
    val fieldNameCase: (Name) -> Name,
    val parameterNameCase: (Name) -> Name,
    val sanitizeSymbol: (String) -> String,
    val extraStatementTransforms: ((Statement, Transformer) -> Statement)? = null,
    val escapeFieldKeywords: Boolean = true,
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
    statementAndExpression { stmt, tr ->
        val extra = config.extraStatementTransforms
        when {
            stmt is FieldCall -> FieldCall(
                receiver = stmt.receiver?.let { tr.transformExpression(it) },
                field = config.sanitizeFieldName(stmt.field),
            )
            extra != null -> extra(stmt, tr)
            else -> stmt.transformChildren(tr)
        }
    }
}

fun SanitizationConfig.sanitizeFieldName(name: Name): Name {
    val cased = fieldNameCase(name)
    val sanitized = sanitizeSymbol(cased.value())
    val escaped = if (escapeFieldKeywords && sanitized in reservedKeywords) escapeKeyword(sanitized) else sanitized
    return Name(listOf(escaped))
}

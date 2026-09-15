package community.flock.wirespec.compiler.core.ir.transformer

import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.Transformer
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren

public data class SanitizationConfig(
    val reservedKeywords: Set<String>,
    val escapeKeyword: (String) -> String,
    val fieldNameCase: (Name) -> Name,
    val parameterNameCase: (Name) -> Name,
    val sanitizeSymbol: (String) -> String,
    val extraStatementTransforms: ((Statement, Transformer) -> Statement)? = null,
    val escapeFieldKeywords: Boolean = true,
)

public fun <T : Element> T.sanitizeNames(config: SanitizationConfig): T = transform {
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

public fun SanitizationConfig.sanitizeFieldName(name: Name): Name {
    val cased = fieldNameCase(name)
    val sanitized = sanitizeSymbol(cased.value())
    val escaped = if (escapeFieldKeywords && sanitized in reservedKeywords) escapeKeyword(sanitized) else sanitized
    return Name(listOf(escaped))
}

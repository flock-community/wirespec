package community.flock.wirespec.ir.generator

import community.flock.wirespec.compiler.core.emit.Keywords

/**
 * The single source of truth for reserved keywords per target language. Generators use these to
 * escape identifiers; the language emitters and extensions delegate to the same objects so a
 * keyword is never defined in more than one place.
 */
object JavaKeywords : Keywords {
    override val reservedKeywords = setOf(
        "abstract", "continue", "for", "new", "switch",
        "assert", "default", "if", "package", "synchronized",
        "boolean", "do", "goto", "private", "this",
        "break", "double", "implements", "protected", "throw",
        "byte", "else", "import", "public", "throws",
        "case", "enum", "instanceof", "return", "transient",
        "catch", "extends", "int", "short", "try",
        "char", "final", "interface", "static", "void",
        "class", "finally", "long", "strictfp", "volatile",
        "const", "float", "native", "super", "while",
        "true", "false", "null",
    )
}

object KotlinKeywords : Keywords {
    override val reservedKeywords = setOf(
        "as", "break", "class", "continue", "do",
        "else", "false", "for", "fun", "if",
        "in", "interface", "internal", "is", "null",
        "object", "open", "package", "return", "super",
        "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "when", "while",
        "private", "public",
    )
}

object ScalaKeywords : Keywords {
    override val reservedKeywords = setOf(
        "abstract", "case", "class", "def", "do",
        "else", "extends", "false", "final", "for",
        "forSome", "if", "implicit", "import", "lazy",
        "match", "new", "null", "object", "override",
        "package", "private", "protected", "return", "sealed",
        "super", "this", "throw", "trait", "true",
        "try", "type", "val", "var", "while",
        "with", "yield", "given", "using", "enum",
        "export", "then",
    )
}

object RustKeywords : Keywords {
    override val reservedKeywords = setOf(
        "as", "break", "const", "continue", "crate",
        "else", "enum", "extern", "false", "fn",
        "for", "if", "impl", "in", "let",
        "loop", "match", "mod", "move", "mut",
        "pub", "ref", "return", "self", "Self",
        "static", "struct", "super", "trait", "true",
        "type", "unsafe", "use", "where", "while",
        "async", "await", "dyn", "abstract", "become",
        "box", "do", "final", "macro", "override",
        "priv", "typeof", "unsized", "virtual", "yield",
        "try",
    )
}

object PythonKeywords : Keywords {
    override val reservedKeywords = setOf(
        "False", "None", "True", "and", "as", "assert",
        "break", "class", "continue", "def", "del",
        "elif", "else", "except", "finally", "for",
        "from", "global", "if", "import", "in",
        "is", "lambda", "nonlocal", "not", "or",
        "pass", "raise", "return", "try", "while",
        "with", "yield",
    )
}

object TypeScriptKeywords : Keywords {
    override val reservedKeywords = setOf(
        "break", "case", "catch", "continue", "debugger",
        "default", "delete", "do", "else", "finally",
        "for", "function", "if", "in", "instanceof",
        "new", "return", "switch", "this", "throw",
        "try", "typeof", "var", "void", "while",
        "with", "class", "const", "enum", "export",
        "extends", "import", "super", "implements",
        "interface", "let", "package", "private",
        "protected", "public", "static", "yield",
        "type", "async", "await",
    )
}

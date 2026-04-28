package community.flock.wirespec.converter.avro

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.converter.avro.AvroConverter.flatten
import community.flock.wirespec.converter.common.Parser

object AvroIdlParser : Parser {

    data class Protocol(
        val name: String,
        val namespace: String?,
        val types: List<AvroModel.Type>,
    )

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST {
        val tokens = AvroIdlTokenizer(moduleContent.content).tokenize()
        val protocol = ProtocolParser(tokens).parseProtocol()
        val definitions: List<Definition> = protocol.types.flatMap { it.flatten() }
        return AST(
            nonEmptyListOf(
                Module(
                    moduleContent.fileUri,
                    definitions.toNonEmptyListOrNull() ?: error("Cannot yield empty AST from protocol ${protocol.name}"),
                ),
            ),
        )
    }

    fun parseProtocol(source: String): Protocol {
        val tokens = AvroIdlTokenizer(source).tokenize()
        return ProtocolParser(tokens).parseProtocol()
    }

    private class ProtocolParser(private val tokens: List<AvroIdlToken>) {
        private var pos = 0

        fun parseProtocol(): Protocol {
            var namespace: String? = null
            while (peekSymbol('@')) {
                val (name, value) = parseAnnotation()
                if (name == "namespace") namespace = value
            }
            expectKeyword("protocol")
            val protocolName = expectIdentifier()
            expectSymbol('{')
            val types = mutableListOf<AvroModel.Type>()
            while (!peekSymbol('}')) {
                types.addAll(parseDeclaration())
            }
            expectSymbol('}')
            if (pos != tokens.size) error("Unexpected tokens after protocol body")
            return Protocol(protocolName, namespace, types)
        }

        private fun parseDeclaration(): List<AvroModel.Type> {
            val doc = consumeDoc()
            while (peekSymbol('@')) parseAnnotation()
            val keyword = peekIdentifierName()
            return when (keyword) {
                "record", "error" -> listOf(parseRecord(doc))
                "enum" -> listOf(parseEnum(doc))
                "fixed" -> {
                    skipFixed()
                    emptyList()
                }
                "import" -> {
                    skipImport()
                    emptyList()
                }
                else -> error("Unexpected token in protocol body: ${peekToken()}")
            }
        }

        private fun parseRecord(doc: String?): AvroModel.RecordType {
            advance()
            val name = expectIdentifier()
            expectSymbol('{')
            val fields = mutableListOf<AvroModel.Field>()
            while (!peekSymbol('}')) {
                fields.add(parseField())
            }
            expectSymbol('}')
            return AvroModel.RecordType(
                type = "record",
                name = name,
                fields = fields,
                doc = doc,
            )
        }

        private fun parseField(): AvroModel.Field {
            val doc = consumeDoc()
            while (peekSymbol('@')) parseAnnotation()
            val type = parseType()
            val name = expectIdentifier()
            val default = if (peekSymbol('=')) {
                advance()
                parseDefaultValue()
            } else {
                null
            }
            expectSymbol(';')
            return AvroModel.Field(
                name = name,
                type = type,
                doc = doc,
                default = default,
            )
        }

        private fun parseDefaultValue(): String {
            val token = peekToken() ?: error("Expected default value")
            return when (token) {
                is AvroIdlToken.StringLiteral -> {
                    advance()
                    token.value
                }
                is AvroIdlToken.NumberLiteral -> {
                    advance()
                    token.value
                }
                is AvroIdlToken.Identifier -> {
                    advance()
                    token.value
                }
                is AvroIdlToken.Symbol -> when (token.value) {
                    '[' -> readMatchedBlock('[', ']')
                    '{' -> readMatchedBlock('{', '}')
                    else -> error("Unexpected symbol in default value: ${token.value}")
                }
                else -> error("Unexpected token in default value: $token")
            }
        }

        private fun readMatchedBlock(open: Char, close: Char): String {
            val parts = mutableListOf<String>()
            var depth = 0
            while (pos < tokens.size) {
                val part = when (val t = tokens[pos]) {
                    is AvroIdlToken.Symbol -> {
                        if (t.value == open) depth++
                        if (t.value == close) depth--
                        "${t.value}"
                    }
                    is AvroIdlToken.Identifier -> t.value
                    is AvroIdlToken.NumberLiteral -> t.value
                    is AvroIdlToken.StringLiteral -> "\"${t.value}\""
                    is AvroIdlToken.DocComment -> ""
                }
                parts += part
                advance()
                if (depth == 0 && part == "$close") return parts.joinToString("")
            }
            error("Unmatched bracket")
        }

        private fun parseType(): AvroModel.TypeList {
            if (peekIdentifierName() == "union") {
                advance()
                expectSymbol('{')
                val types = mutableListOf<AvroModel.Type>()
                types.add(parseSingleType())
                consumeOptionalNullable()
                while (peekSymbol(',')) {
                    advance()
                    types.add(parseSingleType())
                    consumeOptionalNullable()
                }
                expectSymbol('}')
                return AvroModel.TypeList(types)
            }
            val type = parseSingleType()
            if (peekSymbol('?')) {
                advance()
                return AvroModel.TypeList(AvroModel.SimpleType("null"), type)
            }
            return AvroModel.TypeList(type)
        }

        private fun parseSingleType(): AvroModel.Type {
            val token = peekToken() ?: error("Expected type")
            if (token !is AvroIdlToken.Identifier) error("Unexpected token while parsing type: $token")
            return when (token.value) {
                "array" -> {
                    advance()
                    expectSymbol('<')
                    val inner = parseSingleType()
                    consumeOptionalNullable()
                    expectSymbol('>')
                    AvroModel.ArrayType(type = "array", items = inner)
                }
                "map" -> {
                    advance()
                    expectSymbol('<')
                    val inner = parseSingleType()
                    consumeOptionalNullable()
                    expectSymbol('>')
                    AvroModel.MapType(type = "map", values = inner)
                }
                "union" -> {
                    advance()
                    expectSymbol('{')
                    val types = mutableListOf<AvroModel.Type>()
                    types.add(parseSingleType())
                    consumeOptionalNullable()
                    while (peekSymbol(',')) {
                        advance()
                        types.add(parseSingleType())
                        consumeOptionalNullable()
                    }
                    expectSymbol('}')
                    AvroModel.UnionType(name = "", type = AvroModel.TypeList(types))
                }
                else -> {
                    advance()
                    AvroModel.SimpleType(token.value)
                }
            }
        }

        private fun consumeOptionalNullable() {
            if (peekSymbol('?')) advance()
        }

        private fun parseEnum(doc: String?): AvroModel.EnumType {
            advance()
            val name = expectIdentifier()
            expectSymbol('{')
            val symbols = mutableListOf<String>()
            if (!peekSymbol('}')) {
                symbols.add(expectIdentifier())
                while (peekSymbol(',')) {
                    advance()
                    if (peekSymbol('}')) break
                    symbols.add(expectIdentifier())
                }
            }
            expectSymbol('}')
            if (peekSymbol('=')) {
                advance()
                expectIdentifier()
                expectSymbol(';')
            }
            return AvroModel.EnumType(
                type = "enum",
                name = name,
                symbols = symbols,
                doc = doc,
            )
        }

        private fun skipFixed() {
            advance()
            expectIdentifier()
            expectSymbol('(')
            advance()
            expectSymbol(')')
            expectSymbol(';')
        }

        private fun skipImport() {
            advance()
            expectIdentifier()
            if (peekToken() is AvroIdlToken.StringLiteral) advance()
            expectSymbol(';')
        }

        private fun parseAnnotation(): Pair<String, String> {
            expectSymbol('@')
            val name = expectIdentifier()
            expectSymbol('(')
            val token = peekToken() ?: error("Expected annotation value")
            val value = when (token) {
                is AvroIdlToken.StringLiteral -> {
                    advance()
                    token.value
                }
                is AvroIdlToken.Identifier -> {
                    advance()
                    token.value
                }
                else -> error("Unexpected annotation value: $token")
            }
            expectSymbol(')')
            return name to value
        }

        private fun consumeDoc(): String? {
            var last: String? = null
            while (true) {
                val token = peekToken()
                if (token !is AvroIdlToken.DocComment) return last
                last = token.value
                advance()
            }
        }

        private fun peekToken(): AvroIdlToken? = if (pos < tokens.size) tokens[pos] else null

        private fun peekSymbol(c: Char): Boolean {
            val t = peekToken() ?: return false
            return t is AvroIdlToken.Symbol && t.value == c
        }

        private fun peekIdentifierName(): String? {
            val t = peekToken() ?: return null
            return if (t is AvroIdlToken.Identifier) t.value else null
        }

        private fun advance() {
            pos++
        }

        private fun expectIdentifier(): String {
            val t = peekToken() ?: error("Expected identifier, got EOF")
            if (t !is AvroIdlToken.Identifier) error("Expected identifier, got $t")
            advance()
            return t.value
        }

        private fun expectKeyword(keyword: String) {
            val t = peekToken() ?: error("Expected '$keyword', got EOF")
            if (t !is AvroIdlToken.Identifier || t.value != keyword) error("Expected '$keyword', got $t")
            advance()
        }

        private fun expectSymbol(c: Char) {
            val t = peekToken() ?: error("Expected '$c', got EOF")
            if (t !is AvroIdlToken.Symbol || t.value != c) error("Expected '$c', got $t")
            advance()
        }
    }
}

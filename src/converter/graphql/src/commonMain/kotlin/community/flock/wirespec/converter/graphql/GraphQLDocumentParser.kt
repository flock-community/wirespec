package community.flock.wirespec.converter.graphql

import community.flock.wirespec.converter.graphql.GraphQLModel.Argument
import community.flock.wirespec.converter.graphql.GraphQLModel.Definition
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive
import community.flock.wirespec.converter.graphql.GraphQLModel.DirectiveDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Operation
import community.flock.wirespec.converter.graphql.GraphQLModel.OperationTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Value

/** Parses the type system definitions and extensions of a GraphQL document (the SDL). */
internal class GraphQLDocumentParser(private val source: String) {

    private val tokens = GraphQLLexer(source).tokenize()
    private var position = 0

    fun parseDocument(): Document = buildList { while (peek() !is GraphQLToken.EndOfFile) add(parseDefinition()) }
        .takeIf { it.isNotEmpty() }
        ?.let(::Document)
        ?: fail("A GraphQL document must contain at least one definition")

    private fun parseDefinition(): Definition {
        val description = parseDescription()
        return when (val keyword = peekName()) {
            "extend" -> when (description) {
                null -> advance().let { parseTypeSystemDefinition(null, extension = true) }
                else -> fail("An extension cannot have a description")
            }
            "directive" -> parseDirectiveDefinition(description)
            "query", "mutation", "subscription", "fragment" -> fail("Executable definition '$keyword' is not supported, only schema definitions can be converted")
            else -> when {
                peekPunctuator("{") -> fail("Executable definitions are not supported, only schema definitions can be converted")
                else -> parseTypeSystemDefinition(description, extension = false)
            }
        }
    }

    private fun parseTypeSystemDefinition(description: String?, extension: Boolean): Definition = when (val keyword = expectName()) {
        "schema" -> parseSchema(description, extension)
        "scalar" -> ScalarTypeDefinition(description, expectName(), parseDirectives(), extension)
            .also { if (extension && it.directives.isEmpty()) fail("A scalar extension requires directives") }
        "type" -> parseObject(description, extension)
        "interface" -> parseInterface(description, extension)
        "union" -> parseUnion(description, extension)
        "enum" -> parseEnum(description, extension)
        "input" -> parseInput(description, extension)
        else -> fail("Unexpected '$keyword', expected a type system definition")
    }

    private fun parseSchema(description: String?, extension: Boolean): SchemaDefinition {
        val directives = parseDirectives()
        val operationTypes = when {
            peekPunctuator("{") -> parseBlock { parseOperationTypeDefinition() }
            extension && directives.isNotEmpty() -> emptyList()
            else -> fail("Expected '{'")
        }
        return SchemaDefinition(description, directives, operationTypes, extension)
    }

    private fun parseOperationTypeDefinition(): OperationTypeDefinition {
        val keyword = expectName()
        val operation = Operation.entries.find { it.keyword == keyword } ?: fail("Unexpected '$keyword', expected an operation type")
        expectPunctuator(":")
        return OperationTypeDefinition(operation, expectName())
    }

    private fun parseObject(description: String?, extension: Boolean): ObjectTypeDefinition = ObjectTypeDefinition(
        description = description,
        name = expectName(),
        interfaces = parseImplementsInterfaces(),
        directives = parseDirectives(),
        fields = parseOptionalBlock { parseFieldDefinition() },
        extension = extension,
    ).also { if (extension && it.interfaces.isEmpty() && it.directives.isEmpty() && it.fields.isEmpty()) fail("An object extension cannot be empty") }

    private fun parseInterface(description: String?, extension: Boolean): InterfaceTypeDefinition = InterfaceTypeDefinition(
        description = description,
        name = expectName(),
        interfaces = parseImplementsInterfaces(),
        directives = parseDirectives(),
        fields = parseOptionalBlock { parseFieldDefinition() },
        extension = extension,
    ).also { if (extension && it.interfaces.isEmpty() && it.directives.isEmpty() && it.fields.isEmpty()) fail("An interface extension cannot be empty") }

    private fun parseUnion(description: String?, extension: Boolean): UnionTypeDefinition = UnionTypeDefinition(
        description = description,
        name = expectName(),
        directives = parseDirectives(),
        members = when {
            peekPunctuator("=") -> advance().let { parseSeparated("|") { expectName() } }
            else -> emptyList()
        },
        extension = extension,
    ).also { if (extension && it.directives.isEmpty() && it.members.isEmpty()) fail("A union extension cannot be empty") }

    private fun parseEnum(description: String?, extension: Boolean): EnumTypeDefinition = EnumTypeDefinition(
        description = description,
        name = expectName(),
        directives = parseDirectives(),
        values = parseOptionalBlock { parseEnumValueDefinition() },
        extension = extension,
    ).also { if (extension && it.directives.isEmpty() && it.values.isEmpty()) fail("An enum extension cannot be empty") }

    private fun parseEnumValueDefinition(): EnumValueDefinition {
        val description = parseDescription()
        val name = expectName().also { if (it in setOf("true", "false", "null")) fail("'$it' cannot be used as an enum value") }
        return EnumValueDefinition(description, name, parseDirectives())
    }

    private fun parseInput(description: String?, extension: Boolean): InputObjectTypeDefinition = InputObjectTypeDefinition(
        description = description,
        name = expectName(),
        directives = parseDirectives(),
        fields = parseOptionalBlock { parseInputValueDefinition() },
        extension = extension,
    ).also { if (extension && it.directives.isEmpty() && it.fields.isEmpty()) fail("An input extension cannot be empty") }

    private fun parseDirectiveDefinition(description: String?): DirectiveDefinition {
        expectKeyword("directive")
        expectPunctuator("@")
        val name = expectName()
        val arguments = parseArgumentsDefinition()
        val repeatable = (peekName() == "repeatable").also { if (it) advance() }
        expectKeyword("on")
        val locations = parseSeparated("|") { expectName().also { if (it !in DIRECTIVE_LOCATIONS) fail("Unknown directive location '$it'") } }
        return DirectiveDefinition(description, name, arguments, repeatable, locations)
    }

    private fun parseImplementsInterfaces(): List<String> = when (peekName()) {
        "implements" -> advance().let { parseSeparated("&") { expectName() } }
        else -> emptyList()
    }

    private fun parseFieldDefinition(): FieldDefinition {
        val description = parseDescription()
        val name = expectName()
        val arguments = parseArgumentsDefinition()
        expectPunctuator(":")
        return FieldDefinition(description, name, arguments, parseType(), parseDirectives())
    }

    private fun parseArgumentsDefinition(): List<InputValueDefinition> = when {
        peekPunctuator("(") -> parseDelimited("(", ")") { parseInputValueDefinition() }
        else -> emptyList()
    }

    private fun parseInputValueDefinition(): InputValueDefinition {
        val description = parseDescription()
        val name = expectName()
        expectPunctuator(":")
        val type = parseType()
        val defaultValue = when {
            peekPunctuator("=") -> advance().let { parseValue() }
            else -> null
        }
        return InputValueDefinition(description, name, type, defaultValue, parseDirectives())
    }

    private fun parseType(): TypeRef {
        val type = when {
            peekPunctuator("[") -> {
                advance()
                TypeRef.ListOf(parseType()).also { expectPunctuator("]") }
            }
            else -> TypeRef.Named(expectName())
        }
        return when {
            peekPunctuator("!") -> advance().let { TypeRef.NonNull(type) }
            else -> type
        }
    }

    private fun parseDirectives(): List<Directive> = buildList {
        while (peekPunctuator("@")) {
            advance()
            add(Directive(expectName(), parseArguments()))
        }
    }

    private fun parseArguments(): List<Argument> = when {
        peekPunctuator("(") -> parseDelimited("(", ")") { parseArgument() }
        else -> emptyList()
    }

    private fun parseArgument(): Argument {
        val name = expectName()
        expectPunctuator(":")
        return Argument(name, parseValue())
    }

    private fun parseValue(): Value = when (val token = advance()) {
        is GraphQLToken.IntValue -> Value.IntValue(token.raw)
        is GraphQLToken.FloatValue -> Value.FloatValue(token.raw)
        is GraphQLToken.StringValue -> Value.StringValue(token.value)
        is GraphQLToken.Name -> when (token.value) {
            "true" -> Value.BooleanValue(true)
            "false" -> Value.BooleanValue(false)
            "null" -> Value.NullValue
            else -> Value.EnumValue(token.value)
        }
        is GraphQLToken.Punctuator -> when (token.value) {
            "[" -> Value.ListValue(parseUntil("]") { parseValue() })
            "{" -> Value.ObjectValue(parseUntil("}") { parseArgument() })
            "$" -> fail("Variables are not allowed in a schema", token)
            else -> fail("Unexpected '${token.value}', expected a value", token)
        }
        is GraphQLToken.EndOfFile -> fail("Unexpected end of document, expected a value", token)
    }

    private fun parseDescription(): String? = (peek() as? GraphQLToken.StringValue)?.also { advance() }?.value

    private fun <T> parseBlock(item: () -> T): List<T> = parseDelimited("{", "}", item)

    private fun <T> parseOptionalBlock(item: () -> T): List<T> = when {
        peekPunctuator("{") -> parseBlock(item)
        else -> emptyList()
    }

    /** One or more items between [open] and [close]. */
    private fun <T> parseDelimited(open: String, close: String, item: () -> T): List<T> {
        expectPunctuator(open)
        return buildList {
            do add(item()) while (!peekPunctuator(close))
        }.also { advance() }
    }

    /** Zero or more items up to and including [close]; the opening token has already been consumed. */
    private fun <T> parseUntil(close: String, item: () -> T): List<T> = buildList {
        while (!peekPunctuator(close)) add(item())
    }.also { advance() }

    /** One or more items separated by [separator], allowing a leading separator. */
    private fun <T> parseSeparated(separator: String, item: () -> T): List<T> {
        if (peekPunctuator(separator)) advance()
        return buildList {
            add(item())
            while (peekPunctuator(separator)) {
                advance()
                add(item())
            }
        }
    }

    private fun peek(): GraphQLToken = tokens[position]

    private fun peekName(): String? = (peek() as? GraphQLToken.Name)?.value

    private fun peekPunctuator(value: String): Boolean = (peek() as? GraphQLToken.Punctuator)?.value == value

    private fun advance(): GraphQLToken = peek().also { if (it !is GraphQLToken.EndOfFile) position++ }

    private fun expectName(): String = when (val token = peek()) {
        is GraphQLToken.Name -> advance().let { token.value }
        else -> fail("Expected a name, found ${token.describe()}", token)
    }

    private fun expectKeyword(keyword: String) {
        if (peekName() != keyword) fail("Expected '$keyword', found ${peek().describe()}")
        advance()
    }

    private fun expectPunctuator(value: String) {
        if (!peekPunctuator(value)) fail("Expected '$value', found ${peek().describe()}")
        advance()
    }

    private fun GraphQLToken.describe(): String = when (this) {
        is GraphQLToken.Punctuator -> "'$value'"
        is GraphQLToken.Name -> "'$value'"
        is GraphQLToken.IntValue -> "'$raw'"
        is GraphQLToken.FloatValue -> "'$raw'"
        is GraphQLToken.StringValue -> "a string"
        is GraphQLToken.EndOfFile -> "end of document"
    }

    private fun fail(message: String, token: GraphQLToken = peek()): Nothing = source.substring(0, minOf(token.offset, source.length))
        .let { before -> error("$message at line ${before.count { it == '\n' } + 1}, column ${token.offset - before.lastIndexOf('\n')}") }

    private companion object {
        val DIRECTIVE_LOCATIONS = setOf(
            "QUERY", "MUTATION", "SUBSCRIPTION", "FIELD", "FRAGMENT_DEFINITION", "FRAGMENT_SPREAD", "INLINE_FRAGMENT", "VARIABLE_DEFINITION",
            "SCHEMA", "SCALAR", "OBJECT", "FIELD_DEFINITION", "ARGUMENT_DEFINITION", "INTERFACE", "UNION", "ENUM", "ENUM_VALUE",
            "INPUT_OBJECT", "INPUT_FIELD_DEFINITION",
        )
    }
}

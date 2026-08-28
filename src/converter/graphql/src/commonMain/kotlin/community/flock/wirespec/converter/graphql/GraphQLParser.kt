package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.converter.graphql.GraphQLModel.Argument
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeSystemDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Value

object GraphQLParser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST {
        val document = parseDocument(moduleContent.content, strict)
        val definitions = GraphQLConverter.convert(document, strict)
        return AST(
            nonEmptyListOf(
                Module(
                    moduleContent.fileUri,
                    definitions.toNonEmptyListOrNull()
                        ?: error("Cannot yield empty AST from GraphQL schema ${moduleContent.fileUri.value}"),
                ),
            ),
        )
    }

    fun parseDocument(source: String, strict: Boolean = false): Document {
        val tokens = GraphQLTokenizer(source).tokenize()
        return DocumentParser(tokens, strict).parseDocument()
    }

    private class DocumentParser(private val tokens: List<GraphQLToken>, private val strict: Boolean) {
        private var pos = 0

        fun parseDocument(): Document {
            val definitions = mutableListOf<TypeSystemDefinition>()
            while (pos < tokens.size) {
                parseTypeSystemDefinition()?.let(definitions::add)
            }
            return Document(definitions)
        }

        private fun parseTypeSystemDefinition(): TypeSystemDefinition? {
            val description = consumeDescription()
            return when (val keyword = peekName()) {
                "schema" -> parseSchema()
                "type" -> parseObjectLike(description, isInterface = false)
                "interface" -> parseObjectLike(description, isInterface = true)
                "input" -> parseInput(description)
                "enum" -> parseEnum(description)
                "union" -> parseUnion(description)
                "scalar" -> parseScalar(description)
                "directive" -> {
                    skipDirectiveDefinition()
                    null
                }

                "extend" ->
                    if (strict) {
                        error("'extend' definitions are not supported")
                    } else {
                        skipExtension()
                        null
                    }

                "query", "mutation", "subscription", "fragment" ->
                    error("Expected a type system definition but found executable definition '$keyword'")

                else -> error("Unexpected token: ${peekToken()}")
            }
        }

        private fun parseSchema(): SchemaDefinition {
            advance()
            parseDirectives()
            expectPunctuator("{")
            var query: String? = null
            var mutation: String? = null
            var subscription: String? = null
            while (!peekPunctuator("}")) {
                val operation = expectName()
                expectPunctuator(":")
                val typeName = expectName()
                when (operation) {
                    "query" -> query = typeName
                    "mutation" -> mutation = typeName
                    "subscription" -> subscription = typeName
                    else -> error("Unexpected operation type '$operation' in schema definition")
                }
            }
            expectPunctuator("}")
            return SchemaDefinition(query, mutation, subscription)
        }

        private fun parseObjectLike(description: String?, isInterface: Boolean): TypeSystemDefinition {
            advance()
            val name = expectName()
            val interfaces = parseImplements()
            val directives = parseDirectives()
            val fields = parseFieldDefinitions()
            return if (isInterface) {
                InterfaceTypeDefinition(name, description, directives, interfaces, fields)
            } else {
                ObjectTypeDefinition(name, description, directives, interfaces, fields)
            }
        }

        private fun parseInput(description: String?): InputObjectTypeDefinition {
            advance()
            val name = expectName()
            val directives = parseDirectives()
            expectPunctuator("{")
            val fields = mutableListOf<InputValueDefinition>()
            while (!peekPunctuator("}")) {
                fields.add(parseInputValue())
            }
            expectPunctuator("}")
            return InputObjectTypeDefinition(name, description, directives, fields)
        }

        private fun parseEnum(description: String?): EnumTypeDefinition {
            advance()
            val name = expectName()
            val directives = parseDirectives()
            expectPunctuator("{")
            val values = mutableListOf<String>()
            while (!peekPunctuator("}")) {
                consumeDescription()
                values.add(expectName())
                parseDirectives()
            }
            expectPunctuator("}")
            return EnumTypeDefinition(name, description, directives, values)
        }

        private fun parseUnion(description: String?): UnionTypeDefinition {
            advance()
            val name = expectName()
            val directives = parseDirectives()
            expectPunctuator("=")
            val members = mutableListOf(expectName())
            while (peekPunctuator("|")) {
                advance()
                members.add(expectName())
            }
            return UnionTypeDefinition(name, description, directives, members)
        }

        private fun parseScalar(description: String?): ScalarTypeDefinition {
            advance()
            val name = expectName()
            val directives = parseDirectives()
            return ScalarTypeDefinition(name, description, directives)
        }

        private fun parseImplements(): List<String> {
            if (peekName() != "implements") return emptyList()
            advance()
            if (peekPunctuator("&")) advance()
            val interfaces = mutableListOf(expectName())
            while (peekPunctuator("&")) {
                advance()
                interfaces.add(expectName())
            }
            return interfaces
        }

        private fun parseFieldDefinitions(): List<FieldDefinition> {
            expectPunctuator("{")
            val fields = mutableListOf<FieldDefinition>()
            while (!peekPunctuator("}")) {
                val description = consumeDescription()
                val name = expectName()
                val arguments = parseArgumentDefinitions()
                expectPunctuator(":")
                val type = parseTypeRef()
                val directives = parseDirectives()
                fields.add(FieldDefinition(name, description, arguments, type, directives))
            }
            expectPunctuator("}")
            return fields
        }

        private fun parseArgumentDefinitions(): List<InputValueDefinition> {
            if (!peekPunctuator("(")) return emptyList()
            advance()
            val arguments = mutableListOf<InputValueDefinition>()
            while (!peekPunctuator(")")) {
                arguments.add(parseInputValue())
            }
            expectPunctuator(")")
            return arguments
        }

        private fun parseInputValue(): InputValueDefinition {
            val description = consumeDescription()
            val name = expectName()
            expectPunctuator(":")
            val type = parseTypeRef()
            if (peekPunctuator("=")) {
                advance()
                parseValue() // default values are parsed and discarded
            }
            val directives = parseDirectives()
            return InputValueDefinition(name, description, type, directives)
        }

        private fun parseTypeRef(): TypeRef {
            val inner = if (peekPunctuator("[")) {
                advance()
                val element = parseTypeRef()
                expectPunctuator("]")
                TypeRef.ListOf(element, nonNull = false)
            } else {
                TypeRef.Named(expectName(), nonNull = false)
            }
            return if (peekPunctuator("!")) {
                advance()
                when (inner) {
                    is TypeRef.Named -> inner.copy(nonNull = true)
                    is TypeRef.ListOf -> inner.copy(nonNull = true)
                }
            } else {
                inner
            }
        }

        private fun parseDirectives(): List<Directive> {
            val directives = mutableListOf<Directive>()
            while (peekPunctuator("@")) {
                advance()
                val name = expectName()
                val arguments = if (peekPunctuator("(")) {
                    advance()
                    mutableListOf<Argument>().apply {
                        while (!peekPunctuator(")")) {
                            val argName = expectName()
                            expectPunctuator(":")
                            add(Argument(argName, parseValue()))
                        }
                    }.also { expectPunctuator(")") }
                } else {
                    emptyList()
                }
                directives.add(Directive(name, arguments))
            }
            return directives
        }

        private fun parseValue(): Value = when (val token = peekToken()) {
            is GraphQLToken.StringValue -> Value.Single(token.value).also { advance() }
            is GraphQLToken.IntValue -> Value.Single(token.value).also { advance() }
            is GraphQLToken.FloatValue -> Value.Single(token.value).also { advance() }
            is GraphQLToken.Name -> Value.Single(token.value).also { advance() }
            is GraphQLToken.Punctuator -> when (token.value) {
                "[" -> {
                    advance()
                    val values = mutableListOf<Value.Single>()
                    while (!peekPunctuator("]")) {
                        values.add(parseValue() as? Value.Single ?: error("Nested lists in directive arguments are not supported"))
                    }
                    expectPunctuator("]")
                    Value.ListOf(values)
                }

                "{" -> {
                    advance()
                    val fields = mutableListOf<Argument>()
                    while (!peekPunctuator("}")) {
                        val name = expectName()
                        expectPunctuator(":")
                        fields.add(Argument(name, parseValue()))
                    }
                    expectPunctuator("}")
                    Value.ObjectOf(fields)
                }

                "$" -> error("Variables are not allowed in a type system document")
                else -> error("Unexpected token in value position: $token")
            }

            null -> error("Unexpected end of input in value position")
        }

        private fun skipDirectiveDefinition() {
            advance()
            expectPunctuator("@")
            expectName()
            if (peekPunctuator("(")) {
                advance()
                var depth = 1
                while (depth > 0 && pos < tokens.size) {
                    when {
                        peekPunctuator("(") -> depth++
                        peekPunctuator(")") -> depth--
                    }
                    advance()
                }
            }
            if (peekName() == "repeatable") advance()
            if (peekName() == "on") {
                advance()
                if (peekPunctuator("|")) advance()
                expectName()
                while (peekPunctuator("|")) {
                    advance()
                    expectName()
                }
            }
        }

        private fun skipExtension() {
            advance() // extend
            advance() // the definition keyword
            expectName()
            if (peekName() == "implements") parseImplements()
            parseDirectives()
            if (peekPunctuator("=")) {
                advance()
                expectName()
                while (peekPunctuator("|")) {
                    advance()
                    expectName()
                }
                return
            }
            if (peekPunctuator("{")) {
                var depth = 0
                do {
                    when {
                        peekPunctuator("{") -> depth++
                        peekPunctuator("}") -> depth--
                    }
                    advance()
                } while (depth > 0 && pos < tokens.size)
            }
        }

        private fun consumeDescription(): String? = (peekToken() as? GraphQLToken.StringValue)
            ?.also { advance() }
            ?.value

        private fun peekToken(): GraphQLToken? = tokens.getOrNull(pos)

        private fun peekName(): String? = (peekToken() as? GraphQLToken.Name)?.value

        private fun peekPunctuator(value: String): Boolean = (peekToken() as? GraphQLToken.Punctuator)?.value == value

        private fun advance() {
            pos++
        }

        private fun expectName(): String = (peekToken() as? GraphQLToken.Name)?.value
            ?.also { advance() }
            ?: error("Expected a name but found ${peekToken()}")

        private fun expectPunctuator(value: String) {
            require(peekPunctuator(value)) { "Expected '$value' but found ${peekToken()}" }
            advance()
        }
    }
}

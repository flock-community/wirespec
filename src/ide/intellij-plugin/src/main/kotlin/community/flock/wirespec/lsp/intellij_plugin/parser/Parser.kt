package community.flock.wirespec.lsp.intellij_plugin.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import community.flock.wirespec.lsp.intellij_plugin.Language
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.CUSTOM_TYPE
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.ENDPOINT_DEF
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.ENUM_DEF
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.LEFT_CURLY
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.RIGHT_CURLY
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.TYPE_DEF
import community.flock.wirespec.lsp.intellij_plugin.parser.Parser.Body
import community.flock.wirespec.lsp.intellij_plugin.parser.Parser.CustomTypeDef
import community.flock.wirespec.lsp.intellij_plugin.parser.Parser.CustomTypeRef
import community.flock.wirespec.lsp.intellij_plugin.parser.Parser.TypeDef

class Parser : PsiParser {

    object TypeDef : IElementType("TYPE_DEF", Language)
    object EndpointDef : IElementType("ENDPOINT_DEF", Language)
    object CustomTypeDef : IElementType("CUSTOM_TYPE_DEF", Language)
    object CustomTypeRef : IElementType("CUSTOM_TYPE_REF", Language)
    object Body : IElementType("BODY", Language)


    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode = builder.apply {
        mark().also {
            builder.parse()
            it.done(root)
        }
    }.treeBuilt
}

private fun PsiBuilder.parse(): Unit = when {
    eof() -> Unit
    def() -> {
        mark().apply {
            advanceLexer()
            parseDef()
            done(TypeDef)
        }
        parse()
    }

    else -> {
        advanceLexer()
        parse()
    }
}

private fun PsiBuilder.def() = when (tokenType) {
    TYPE_DEF, ENDPOINT_DEF, ENUM_DEF -> true
    else -> false
}

private fun PsiBuilder.parseDef(): Unit = when {
    eof() -> Unit
    def() -> Unit
    tokenType == CUSTOM_TYPE -> {
        mark().apply {
            advanceLexer()
            done(CustomTypeDef)
        }
        parseRef()
    }

    else -> {
        advanceLexer()
        parseDef()
    }
}

private fun PsiBuilder.parseRef(): Unit = when {
    eof() -> Unit
    def() -> Unit
    tokenType == CUSTOM_TYPE -> {
        mark().apply {
            advanceLexer()
            done(CustomTypeRef)
        }
        parseRef()
    }

    tokenType == LEFT_CURLY -> {
        mark().apply {
            advanceLexer()
            parseBody()
            done(Body)
        }
        advanceLexer()
        parseRef()
    }

    else -> {
        advanceLexer()
        parseRef()
    }
}

private fun PsiBuilder.parseBody(): Unit = when {
    eof() -> Unit
    def() -> Unit
    tokenType == RIGHT_CURLY -> Unit
    tokenType == CUSTOM_TYPE -> {
        mark().apply {
            advanceLexer()
            done(CustomTypeRef)
        }
        parseBody()
    }

    else -> {
        advanceLexer()
        parseBody()
    }
}

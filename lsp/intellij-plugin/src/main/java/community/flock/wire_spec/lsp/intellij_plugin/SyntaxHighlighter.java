package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SyntaxHighlighter extends SyntaxHighlighterBase {

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new LexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(Types.KEYWORD)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.KEYWORD);
        }
        if (tokenType.equals(Types.VALUE)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.IDENTIFIER);
        }
        if (tokenType.equals(Types.TYPE)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.PARAMETER);
        }
        if (tokenType.equals(Types.BRACKETS)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.BRACKETS);
        }
        if (tokenType.equals(Types.COLON)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.SEMICOLON);
        }
        if (tokenType.equals(Types.COMMA)) {
            return toTextAttributesKeyArray(DefaultLanguageHighlighterColors.COMMA);
        }
        return new TextAttributesKey[0];
    }

    public TextAttributesKey @NotNull [] toTextAttributesKeyArray(TextAttributesKey in){
        return new TextAttributesKey[]{in};
    }
}

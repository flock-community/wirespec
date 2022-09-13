package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;
import community.flock.wirespec.compiler.core.Either;
import community.flock.wirespec.compiler.core.EitherKt;
import community.flock.wirespec.compiler.core.WireSpec;
import community.flock.wirespec.compiler.core.exceptions.WireSpecException;
import community.flock.wirespec.compiler.core.tokenize.Token;
import community.flock.wirespec.compiler.core.tokenize.TokenizerKt;
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram;
import community.flock.wirespec.compiler.core.tokenize.types.Keyword;
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly;
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly;
import community.flock.wirespec.compiler.core.tokenize.types.WsType;
import community.flock.wirespec.compiler.core.tokenize.types.Comma;
import community.flock.wirespec.compiler.core.tokenize.types.Colon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LexerAdapter extends Lexer {

  private CharSequence buffer;
  private int index;
  private List<Token> tokens;

  @Override
  public void start(@NotNull final CharSequence buffer, final int startOffset, final int endOffset, final int initialState) {

    this.buffer = buffer;
    index = 0;

    if(buffer.length() > 0){
      Either<WireSpecException.CompilerException, List<Token>> tokenize = TokenizerKt.tokenize(WireSpec.INSTANCE, buffer.toString());
      tokens = EitherKt.orNull(tokenize);
      assert tokens != null;
      tokens = tokens.stream()
              .filter(it -> !it.getType().equals(EndOfProgram.INSTANCE))
              .toList();
    }
  }

  @NotNull
  @Override
  public CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public IElementType getTokenType() {
    if(index == tokens.size()){
      return null;
    } else {
      Token token = tokens.get(index);
      if (token.getType() instanceof WsType) {
        return Types.TYPE;
      }
      if (token.getType() instanceof Keyword) {
        return Types.KEYWORD;
      }
      if (token.getType() instanceof RightCurly || token.getType() instanceof LeftCurly) {
        return Types.BRACKETS;
      }
      if (token.getType() instanceof Colon) {
        return Types.COLON;
      }
      if (token.getType() instanceof Comma) {
        return Types.COMMA;
      }
      else {
        return Types.VALUE;
      }
    }
  }

  @Override
  public int getTokenStart() {
    Token token = tokens.get(index);
    return token.getCoordinates().getIdxAndLength().getIdx() - token.getCoordinates().getIdxAndLength().getLength();
  }

  @Override
  public int getTokenEnd() {
    Token token = tokens.get(index);
    return token.getCoordinates().getIdxAndLength().getIdx();
  }

  @Override
  public void advance() {
    index++;
  }

  @NotNull
  @Override
  public LexerPosition getCurrentPosition() {
    Token token = tokens.get(index);
    int pos = token.getCoordinates().getIdxAndLength().getIdx() - token.getCoordinates().getIdxAndLength().getLength();
    return new LexerPositionImpl(pos, getState());
  }

  @Override
  public void restore(@NotNull LexerPosition position) {
  }

  @Override
  public int getBufferEnd() {
    return buffer.toString().length();
  }

  static class LexerPositionImpl implements LexerPosition {
    private final int myOffset;
    private final int myState;

    LexerPositionImpl(final int offset, final int state) {
      myOffset = offset;
      myState = state;
    }

    @Override
    public int getOffset() {
      return myOffset;
    }

    @Override
    public int getState() {
      return 1;
    }
  }
}

// This is a generated file. Not intended for manual editing.
package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface Types {

  IElementType BRACKETS = new ElementType("BRACKETS");
  IElementType TYPE = new ElementType("TYPE");
  IElementType KEYWORD = new ElementType("KEYWORD");
  IElementType VALUE = new ElementType("VALUE");
  IElementType COLON = new ElementType("COLON");
  IElementType COMMA = new ElementType("COMMA");

  class ElementType extends IElementType {

    public ElementType(@NotNull @NonNls String debugName) {
      super(debugName, Language.INSTANCE);
    }

  }
}

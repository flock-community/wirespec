package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SyntaxHighlighterFactory extends com.intellij.openapi.fileTypes.SyntaxHighlighterFactory {

    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return new SyntaxHighlighter();
    }

}
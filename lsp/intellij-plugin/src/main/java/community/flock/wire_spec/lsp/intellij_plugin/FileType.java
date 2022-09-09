package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FileType extends LanguageFileType {

    public static final FileType INSTANCE = new FileType();

    private FileType() {
        super(Language.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "wire-spec";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "wire-spec language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "ws";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.FILE;
    }

}

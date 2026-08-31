package community.flock.wirespec.ide.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

public object FileType : LanguageFileType(Language) {
    override fun getName(): String = "wirespec"

    override fun getDescription(): String = "Wirespec language file"

    override fun getDefaultExtension(): String = "ws"

    override fun getIcon(): Icon = Icons.FILE
}

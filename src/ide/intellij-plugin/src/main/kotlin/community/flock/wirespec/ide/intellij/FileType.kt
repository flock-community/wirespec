package community.flock.wirespec.ide.intellij

import com.intellij.openapi.fileTypes.LanguageFileType

object FileType : LanguageFileType(Language) {
    override fun getName() = "wirespec"

    override fun getDescription() = "Wirespec language file"

    override fun getDefaultExtension() = "ws"

    override fun getIcon() = Icons.FILE
}

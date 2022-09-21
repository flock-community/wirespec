package community.flock.wire_spec.lsp.intellij_plugin

import javax.swing.Icon

class FileType private constructor() :
    com.intellij.openapi.fileTypes.LanguageFileType(Language.INSTANCE) {
    override fun getName(): String {
        return "wire-spec"
    }

    override fun getDescription(): String {
        return "wire-spec language file"
    }

    override fun getDefaultExtension(): String {
        return "ws"
    }

    override fun getIcon(): Icon {
        return Icons.FILE
    }

    companion object{
        val INSTANCE: FileType = FileType()
    }

}
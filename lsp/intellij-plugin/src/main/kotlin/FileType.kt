package community.flock.wire_spec.lsp.intellij_plugin

class FileType private constructor() :
    com.intellij.openapi.fileTypes.LanguageFileType(Language.INSTANCE) {
    override fun getName() = "wire-spec"

    override fun getDescription() = "wire-spec language file"

    override fun getDefaultExtension() = "ws"
    override fun getIcon() = Icons.FILE

    companion object {
        val INSTANCE: FileType = FileType()
    }

}
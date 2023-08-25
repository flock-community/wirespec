package community.flock.wirespec.lsp.intellij_plugin

class FileType private constructor() :
    com.intellij.openapi.fileTypes.LanguageFileType(Language.INSTANCE) {
    override fun getName() = "wirespec"

    override fun getDescription() = "wirespec language file"

    override fun getDefaultExtension() = "ws"
    override fun getIcon() = Icons.FILE

    companion object {
        val INSTANCE: FileType = FileType()
    }

}
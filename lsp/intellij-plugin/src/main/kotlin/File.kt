package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.openapi.fileTypes.FileType as IntellijFileType

class File(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, Language.INSTANCE) {

    override fun getFileType(): IntellijFileType = FileType.INSTANCE

    override fun toString(): String = "Wire-spec File"

}
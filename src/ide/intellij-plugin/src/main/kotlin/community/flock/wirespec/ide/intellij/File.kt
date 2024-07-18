package community.flock.wirespec.ide.intellij

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.openapi.fileTypes.FileType as IntellijFileType

class File(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, Language) {

    override fun getFileType(): IntellijFileType = FileType

    override fun toString(): String = "Wirespec File"

}

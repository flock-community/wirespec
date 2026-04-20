package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.File

fun interface TestIrExtension {
    fun transformTestFile(file: File): File
}

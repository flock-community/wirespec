package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual abstract class File actual constructor(actual val path: Path) {

    actual fun read(): String = fs.readFileSync(path.fullFilePath, "utf-8").unsafeCast<String>()

    actual fun write(text: String) {
        fs.writeFileSync(path.fullFilePath.split(".").joinToString("-from-js."), text)
    }

}

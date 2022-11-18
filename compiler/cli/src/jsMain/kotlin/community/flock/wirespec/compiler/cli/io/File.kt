package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual abstract class File actual constructor(actual val path: DirPath) : Copy {

    actual fun read(): String = fs.readFileSync(path.fullFilePath, "utf-8").unsafeCast<String>()

    actual fun write(text: String) {
        if (!fs.existsSync(path.directory, """{ recursive: true }""").unsafeCast<Boolean>()) {
            fs.mkdirSync(path.directory)
        }
        fs.writeFileSync(path.fullFilePath.split(".").joinToString("-from-js."), text)
    }

}

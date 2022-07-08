package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual abstract class AbstractFile actual constructor(private val path: String) {

    actual fun read(): String = fs.readFileSync(path, "utf-8").unsafeCast<String>()

    actual fun write(text: String) {
        fs.writeFileSync(path.split(".").joinToString("-from-js."), text)
    }

}

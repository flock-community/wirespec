package community.flock.wirespec.io

import community.flock.wirespec.js.fs

actual abstract class AbstractFile actual constructor(private val path: String) {

    actual fun read(): String = fs.readFileSync(path, "utf-8").unsafeCast<String>()

    actual fun write(text: String) {
        fs.writeFileSync(path.split(".").joinToString("-from-js."), text)
    }

}

package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.Writer
import community.flock.wirespec.plugin.cli.js.fs

actual abstract class File actual constructor(actual val path: FullFilePath) :
    Reader,
    Writer,
    Copy {

    actual override fun read(): String = fs.readFileSync(path.toString(), "utf-8").unsafeCast<String>()

    actual override fun write(string: String) {
        path.copy(directory = path.directory.split("out").joinToString("out/node")).run {
            if (!fs.existsSync(directory).unsafeCast<Boolean>()) {
                fs.mkdirSync(directory, js("{ recursive: true }"))
            }
            fs.writeFileSync(this.toString(), string)
        }
    }
}

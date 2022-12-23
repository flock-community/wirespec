package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual abstract class File actual constructor(actual val path: FullFilePath) : Copy {

    actual fun read(): String = fs.readFileSync(path.toString(), "utf-8").unsafeCast<String>()

    actual fun write(text: String) {
        path.copy(directory = path.directory.split("out").joinToString("out/node")).run {
            if (!fs.existsSync(directory).unsafeCast<Boolean>()) {
                fs.mkdirSync(directory, js("{ recursive: true }"))
            }
            fs.writeFileSync(this.toString(), text)
        }
    }

}

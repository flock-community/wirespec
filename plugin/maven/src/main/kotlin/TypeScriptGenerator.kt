package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.utils.Logger
import java.io.File
import java.util.Locale

class TypeScriptGenerator(targetDirectory: String, version: String, scalars: Map<String, String> = mapOf(), logger: Logger) : Generator(
    languageDirectory = "$targetDirectory$typeDir",
    pathTemplate = { languageDirectory -> { fileName -> "$languageDirectory/${fileName.lowercase(Locale.getDefault())}.d.ts" } },
    emitter = TypeScriptEmitter(logger),
    disclaimer = "// This is generated code\n// DO NOT MODIFY\n// It will be overwritten\n\n",
    logger = logger
) {

    init {
        File("$targetDirectory$typeDir/package.json").writeText("{\n  \"name\": \"graphql-simple-bindings\",\n  \"version\": \"$version\"\n}\n")
    }

    companion object {
        const val typeDir = "/TypeScript"
    }

}

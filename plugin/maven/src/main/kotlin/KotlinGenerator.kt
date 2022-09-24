package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.utils.Logger


class KotlinGenerator(
    targetDirectory: String,
    packageName: String,
    scalars: Map<String, String>,
    enableOpenApiAnnotations: Boolean,
    logger:Logger
) : Generator(
    languageDirectory = "$targetDirectory/${packageName.split(".").joinToString("/")}",
    pathTemplate = { languageDirectory -> { fileName -> "$languageDirectory/${fileName.capitalize()}.kt" } },
    emitter = KotlinEmitter(logger),
    disclaimer = "/**\n * This is generated code\n * DO NOT MODIFY\n * It will be overwritten\n */\n\n",
    logger = logger
)

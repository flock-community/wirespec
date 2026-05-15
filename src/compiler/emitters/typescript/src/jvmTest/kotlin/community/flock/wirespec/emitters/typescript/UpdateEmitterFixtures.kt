package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.generator.TypeScriptGenerator

fun main(args: Array<String>) {
    val factory = { TypeScriptIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        language = "typescript",
        fixtures = EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    TypeScriptIrEmitter().emitShared()
                        ?.let(TypeScriptGenerator::generate)
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

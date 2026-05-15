package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.generator.KotlinGenerator

fun main(args: Array<String>) {
    val factory = { KotlinIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        language = "kotlin",
        fixtures = EmitterFixtureUpdater.nodeFixtures(factory) +
            EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    KotlinIrEmitter(emitShared = EmitShared(true)).emitShared()
                        ?.let(KotlinGenerator::generate)
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.generator.JavaGenerator

fun main(args: Array<String>) {
    val factory = { JavaIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        fixtures = EmitterFixtureUpdater.nodeFixtures(factory) +
            EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    JavaIrEmitter(emitShared = EmitShared(true)).emitShared()
                        ?.let(JavaGenerator::generate)
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

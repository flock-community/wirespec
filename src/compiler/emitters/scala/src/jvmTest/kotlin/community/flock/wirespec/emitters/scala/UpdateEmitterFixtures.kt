package community.flock.wirespec.emitters.scala

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.generator.ScalaGenerator

fun main(args: Array<String>) {
    val factory = { ScalaIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        language = "scala",
        fixtures = EmitterFixtureUpdater.nodeFixtures(factory) +
            EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    ScalaIrEmitter(emitShared = EmitShared(true)).emitShared()
                        ?.let(ScalaGenerator::generate)
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

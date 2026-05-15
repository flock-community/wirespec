package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.core.RawElement

fun main(args: Array<String>) {
    val factory = { PythonIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        language = "python",
        fixtures = EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    PythonIrEmitter(emitShared = EmitShared(true)).emitShared()
                        ?.elements
                        ?.filterIsInstance<RawElement>()
                        ?.joinToString("") { it.code }
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

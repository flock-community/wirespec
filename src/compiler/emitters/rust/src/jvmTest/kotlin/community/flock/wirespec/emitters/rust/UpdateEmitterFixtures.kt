package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.EmitterFixtureUpdater
import community.flock.wirespec.ir.core.RawElement

fun main(args: Array<String>) {
    val factory = { RustIrEmitter() }
    EmitterFixtureUpdater.run(
        args = args,
        language = "rust",
        fixtures = EmitterFixtureUpdater.compileFixtures(factory) +
            mapOf(
                "sharedOutputTest" to {
                    RustIrEmitter(emitShared = EmitShared(true)).emitShared()
                        ?.elements
                        ?.filterIsInstance<RawElement>()
                        ?.joinToString("") { it.code }
                        ?: error("Shared emit returned null")
                },
            ),
    )
}

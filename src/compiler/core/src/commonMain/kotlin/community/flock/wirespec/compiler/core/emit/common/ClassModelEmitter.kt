package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.utils.Logger

abstract class ClassModelEmitter(logger: Logger, split: Boolean) : Emitter(logger, split) {

    fun String.spacer(space: Int = 1) = this
        .split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { SPACER }}$it"
            } else {
                it
            }
        }

}
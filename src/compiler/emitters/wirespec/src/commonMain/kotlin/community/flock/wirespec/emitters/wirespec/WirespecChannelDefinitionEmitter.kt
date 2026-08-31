package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Channel

internal interface WirespecChannelDefinitionEmitter: ChannelDefinitionEmitter, WirespecTypeDefinitionEmitter {
    override fun emit(channel: Channel): String =
        "channel ${emit(channel.identifier)} -> ${channel.reference.emit()}"
}

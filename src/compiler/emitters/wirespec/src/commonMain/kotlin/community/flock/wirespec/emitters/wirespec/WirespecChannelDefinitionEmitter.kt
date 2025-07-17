package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Channel

interface WirespecChannelDefinitionEmitter: ChannelDefinitionEmitter, IdentifierEmitter, TypeDefinitionEmitter {

    override fun emit(channel: Channel): String =
        "channel ${emit(channel.identifier)} -> ${channel.reference.emit()}"

}
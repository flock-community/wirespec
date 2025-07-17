package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Channel

interface PythonChannelDefinitionEmitter: ChannelDefinitionEmitter {

    override fun emit(channel: Channel) = ""

}
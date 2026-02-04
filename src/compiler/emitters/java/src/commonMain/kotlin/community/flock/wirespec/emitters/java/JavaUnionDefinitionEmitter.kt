package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.generator.generateJava

interface JavaUnionDefinitionEmitter : UnionDefinitionEmitter, JavaIdentifierEmitter {

    override fun emit(union: Union) = union
        .convert()
        .generateJava()
        .run { this + "\n" }
}

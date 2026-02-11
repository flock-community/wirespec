package community.flock.wirespec.compiler.testupdater

import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.Definition

data class NodeFixtureTest(
    val methodName: String,
    val fixtureName: String,
    val node: Definition,
)

data class CompileTest(
    val methodName: String,
    val fixtureName: String,
    val source: String,
)

data class EmitterTestConfig(
    val packageName: String,
    val className: String,
    val emitterFactory: () -> Emitter,
    val emitterConstructor: String,
    val outputPath: String,
    val nodeFixtureTests: List<NodeFixtureTest>,
    val compileTests: List<CompileTest>,
    val sharedSource: String? = null,
)

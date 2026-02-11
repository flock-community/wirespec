package community.flock.wirespec.compiler.testupdater

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import java.io.File

private val projectRoot = findProjectRoot()

private fun findProjectRoot(): File {
    var dir = File(System.getProperty("user.dir"))
    while (dir.parentFile != null) {
        if (File(dir, "settings.gradle.kts").exists()) return dir
        dir = dir.parentFile
    }
    error("Could not find project root (no settings.gradle.kts found)")
}

val allCompileTests = listOf(
    CompileTest("compileFullEndpointTest", "FullEndpoint", CompileFullEndpointTest.source),
    CompileTest("compileChannelTest", "Channel", CompileChannelTest.source),
    CompileTest("compileEnumTest", "Enum", CompileEnumTest.source),
    CompileTest("compileMinimalEndpointTest", "MinimalEndpoint", CompileMinimalEndpointTest.source),
    CompileTest("compileRefinedTest", "Refined", CompileRefinedTest.source),
    CompileTest("compileUnionTest", "Union", CompileUnionTest.source),
    CompileTest("compileTypeTest", "Type", CompileTypeTest.source),
)

val nodeFixtureTests = listOf(
    NodeFixtureTest("testEmitterType", "type", NodeFixtures.type),
    NodeFixtureTest("testEmitterEmptyType", "emptyType", NodeFixtures.emptyType),
    NodeFixtureTest("testEmitterRefined", "refined", NodeFixtures.refined),
    NodeFixtureTest("testEmitterEnum", "enum", NodeFixtures.enum),
)

val configs = listOf(
    EmitterTestConfig(
        packageName = "community.flock.wirespec.emitters.java",
        className = "JavaIrEmitterTest",
        emitterFactory = { JavaIrEmitter() },
        emitterConstructor = "JavaIrEmitter()",
        outputPath = "src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt",
        nodeFixtureTests = nodeFixtureTests,
        compileTests = allCompileTests,
        sharedSource = JavaIrEmitter().shared?.source,
    ),
    EmitterTestConfig(
        packageName = "community.flock.wirespec.emitters.kotlin",
        className = "KotlinIrEmitterTest",
        emitterFactory = { KotlinIrEmitter() },
        emitterConstructor = "KotlinIrEmitter()",
        outputPath = "src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt",
        nodeFixtureTests = nodeFixtureTests,
        compileTests = allCompileTests,
        sharedSource = KotlinIrEmitter().shared?.source,
    ),
    EmitterTestConfig(
        packageName = "community.flock.wirespec.emitters.typescript",
        className = "TypeScriptIrEmitterTest",
        emitterFactory = { TypeScriptIrEmitter() },
        emitterConstructor = "TypeScriptIrEmitter()",
        outputPath = "src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt",
        nodeFixtureTests = emptyList(),
        compileTests = allCompileTests,
        sharedSource = TypeScriptIrEmitter().shared?.source,
    ),
    EmitterTestConfig(
        packageName = "community.flock.wirespec.emitters.python",
        className = "PythonIrEmitterTest",
        emitterFactory = { PythonIrEmitter() },
        emitterConstructor = "PythonIrEmitter()",
        outputPath = "src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt",
        nodeFixtureTests = emptyList(),
        compileTests = allCompileTests,
        sharedSource = PythonIrEmitter().shared?.source,
    ),
    EmitterTestConfig(
        packageName = "community.flock.wirespec.emitters.wirespec",
        className = "WirespecEmitterTest",
        emitterFactory = { WirespecEmitter() },
        emitterConstructor = "WirespecEmitter()",
        outputPath = "src/compiler/emitters/wirespec/src/commonTest/kotlin/community/flock/wirespec/emitters/wirespec/WirespecEmitterTest.kt",
        nodeFixtureTests = emptyList(),
        compileTests = allCompileTests,
    ),
)

fun main() {
    println("Regenerating emitter test files...")
    for (config in configs) {
        print("  ${config.className}... ")
        val content = generateTestFile(config)
        val outputFile = File(projectRoot, config.outputPath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)
        println("OK (${outputFile.absolutePath})")
    }
    println("Done! ${configs.size} test files regenerated.")
}

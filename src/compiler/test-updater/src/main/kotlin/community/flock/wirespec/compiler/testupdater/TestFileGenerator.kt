package community.flock.wirespec.compiler.testupdater

fun generateTestFile(config: EmitterTestConfig): String {
    val emitter = config.emitterFactory()
    val sb = StringBuilder()

    sb.appendLine("package ${config.packageName}")
    sb.appendLine()

    // Collect imports
    val imports = mutableSetOf<String>()
    if (config.nodeFixtureTests.isNotEmpty()) {
        imports.add("import arrow.core.nonEmptyListOf")
        imports.add("import arrow.core.nonEmptySetOf")
        imports.add("import community.flock.wirespec.compiler.core.EmitContext")
        imports.add("import community.flock.wirespec.compiler.core.FileUri")
        imports.add("import community.flock.wirespec.compiler.core.parse.ast.AST")
        imports.add("import community.flock.wirespec.compiler.core.parse.ast.Definition")
        imports.add("import community.flock.wirespec.compiler.core.parse.ast.Module")
        imports.add("import community.flock.wirespec.compiler.test.NodeFixtures")
        imports.add("import community.flock.wirespec.compiler.utils.NoLogger")
        imports.add("import io.kotest.matchers.shouldBe")
    }
    if (config.compileTests.isNotEmpty()) {
        val compileFixtures = config.compileTests.map { it.fixtureName }.toSet()
        for (fixture in compileFixtures) {
            imports.add("import community.flock.wirespec.compiler.test.Compile${fixture}Test")
        }
        imports.add("import io.kotest.assertions.arrow.core.shouldBeRight")
    }
    if (config.sharedSource != null) {
        imports.add("import io.kotest.matchers.shouldBe")
    }
    imports.add("import kotlin.test.Test")

    for (imp in imports.sorted()) {
        sb.appendLine(imp)
    }
    sb.appendLine()

    sb.appendLine("class ${config.className} {")

    // EmitContext for node fixture tests
    if (config.nodeFixtureTests.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("    private val emitContext = object : EmitContext, NoLogger {")
        sb.appendLine("        override val emitters = nonEmptySetOf(${config.emitterConstructor})")
        sb.appendLine("    }")
    }

    // Node fixture tests
    for (test in config.nodeFixtureTests) {
        sb.appendLine()
        val results = runNodeFixture(emitter, test.node)
        sb.appendLine("    @Test")
        sb.appendLine("    fun ${test.methodName}() {")
        sb.appendLine("        val expected = listOf(")
        for ((i, result) in results.withIndex()) {
            val escaped = escapeForRawString(result)
            sb.appendLine("            \"\"\"")
            sb.appendLine(escaped)
            sb.appendLine("            \"\"\".trimMargin(),")
        }
        sb.appendLine("        )")
        sb.appendLine()
        sb.appendLine("        val res = emitContext.emitFirst(NodeFixtures.${test.fixtureName})")
        sb.appendLine("        res shouldBe expected")
        sb.appendLine("    }")
    }

    // Compile tests
    for (test in config.compileTests) {
        sb.appendLine()
        val result = runCompile(test.source, config.emitterFactory())
        val escaped = escapeForRawString(result)
        sb.appendLine("    @Test")
        sb.appendLine("    fun ${test.methodName}() {")

        val varName = config.packageName.substringAfterLast(".")
        sb.appendLine("        val $varName = \"\"\"")
        sb.appendLine(escaped)
        sb.appendLine("        \"\"\".trimMargin()")
        sb.appendLine()
        sb.appendLine("        Compile${test.fixtureName}Test.compiler { ${config.emitterConstructor} } shouldBeRight $varName")
        sb.appendLine("    }")
    }

    // Shared output test
    config.sharedSource?.let { source ->
        sb.appendLine()
        val escaped = escapeForRawString(source)
        sb.appendLine("    @Test")
        sb.appendLine("    fun sharedOutputTest() {")
        sb.appendLine("        val expected = \"\"\"")
        sb.appendLine(escaped)
        sb.appendLine("        \"\"\".trimMargin()")
        sb.appendLine()
        sb.appendLine("        val emitter = ${config.emitterConstructor}")
        sb.appendLine("        emitter.shared!!.source shouldBe expected")
        sb.appendLine("    }")
    }

    // Helper method for node fixtures
    if (config.nodeFixtureTests.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("    private fun EmitContext.emitFirst(node: Definition) = emitters.map {")
        sb.appendLine("        val ast = AST(")
        sb.appendLine("            nonEmptyListOf(")
        sb.appendLine("                Module(")
        sb.appendLine("                    FileUri(\"\"),")
        sb.appendLine("                    nonEmptyListOf(node),")
        sb.appendLine("                ),")
        sb.appendLine("            ),")
        sb.appendLine("        )")
        sb.appendLine("        it.emit(ast, logger).first().result")
        sb.appendLine("    }")
    }

    sb.appendLine("}")
    sb.appendLine()

    return sb.toString()
}

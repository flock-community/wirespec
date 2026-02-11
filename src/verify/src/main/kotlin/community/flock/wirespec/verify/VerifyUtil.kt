package community.flock.wirespec.verify

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.test.Fixture
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.FunctionCall
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Main
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.transformChildren
import community.flock.wirespec.language.core.transformer
import community.flock.wirespec.language.generator.generateJava
import community.flock.wirespec.language.generator.generateKotlin
import community.flock.wirespec.language.generator.generatePython
import community.flock.wirespec.language.generator.generateTypeScript
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.io.File
import community.flock.wirespec.language.core.File as AstFile

class Language(
    val name: String,
    val emitter: Emitter,
    val image: () -> String,
) {
    override fun toString() = name
    lateinit var container: GenericContainer<*>
    private lateinit var outputDir: File
    private lateinit var fixture: Fixture

    private val tsExtraFiles: (File) -> Unit = { outputDir ->
        File(outputDir, "tsconfig.json").writeText(
            """
                {
                  "compilerOptions": {
                    "strict": true,
                    "noEmit": true,
                    "skipLibCheck": true,
                    "target": "ES2019",
                    "module": "CommonJS",
                    "moduleResolution": "node"
                  },
                  "include": ["./**/*.ts"]
                }
                """.trimIndent()
        )
    }

    fun generate(file: AstFile, outputDir: File) {
        val (fileName, content) = when (emitter) {
            is JavaIrEmitter -> "${file.name}.java" to file.generateJava()
            is KotlinIrEmitter -> "${file.name}.kt" to file.generateKotlin()
            is PythonIrEmitter -> "${file.name}.py" to file.generatePython()
            is TypeScriptIrEmitter -> "${file.name}.ts" to file.generateTypeScript()
            else -> error("Unknown language: $name")
        }
        outputDir.resolve(fileName).writeText(content)
    }

    fun start(name: String, fixture: Fixture, extraFiles: (File) -> Unit = {}) {
        this.fixture = fixture
        val (cont, dir) = compileAndVerify(
            name = name,
            emitter = emitter,
            fixture = fixture,
            language = this.name.lowercase(),
            image = image(),
            extraFiles = { dir ->
                tsExtraFiles(dir)
                extraFiles(dir)
            },
        )
        container = cont
        outputDir = dir
    }

    fun compile() {
        val verifyCommand = when (emitter) {
            is JavaIrEmitter -> "find /app/gen -name '*.java' | xargs javac -d /tmp/out"
            is KotlinIrEmitter -> "/opt/kotlinc/bin/kotlinc -nowarn -include-runtime /app/gen/ -d /tmp/run.jar"
            is PythonIrEmitter -> "python -m mypy --disable-error-code=empty-body /app/gen/"
            is TypeScriptIrEmitter -> "npm install -g typescript && cd /app/gen && tsc --noEmit"
            else -> error("Unknown language: ${emitter::class.simpleName}")
        }
        exec(verifyCommand)
    }

    fun run(testFile: AstFile) {
        val resolved = if (emitter is TypeScriptIrEmitter) testFile.adaptForTypeScript(fixture) else testFile
        generate(resolved, outputDir)
        compile()
        val fileName = testFile.name
        val runCommand: String = when (emitter) {
            is JavaIrEmitter -> "java -ea -cp /tmp/out $fileName"
            is KotlinIrEmitter -> "java -ea -cp /tmp/run.jar ${fileName}Kt"
            is PythonIrEmitter -> "cd /app/gen && python -O ${fileName}.py"
            is TypeScriptIrEmitter -> "npm install -g tsx && cd /app/gen && tsx ${fileName}.ts"
            else -> error("Unknown language: ${name}")
        }
        exec(runCommand)
    }

    fun exec(command: String) {
        val result = container.execInContainer("sh", "-c", command)
        if (result.stdout.isNotBlank()) {
            println("=== stdout ===")
            println(result.stdout)
        }
        if (result.stderr.isNotBlank()) {
            println("=== stderr ===")
            println(result.stderr)
        }
        if(result.exitCode != 0){
            println("=== exit code ===")
            println(result.exitCode)
        }
        result.exitCode shouldBe 0
    }
}

fun compileAndVerify(
    name: String,
    emitter: Emitter,
    fixture: Fixture,
    language: String,
    image: String,
    extraFiles: (File) -> Unit = {},
): Pair<GenericContainer<*>, File> {
    val emitted = object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter)
    }.compile(nonEmptyListOf(ModuleContent(FileUri("N/A"), fixture.source)))

    val files = emitted.fold(
        { error -> throw AssertionError("Compilation failed: $error") },
        { it }
    )

    val outputDir = File(System.getProperty("buildDir"), "generated/$name/$language")
    outputDir.deleteRecursively()
    outputDir.mkdirs()
print(outputDir.absolutePath)
    files.forEach { file ->
        val target = File(outputDir, file.file)
        target.parentFile.mkdirs()
        target.writeText(file.result)
    }

    extraFiles(outputDir)

    val container = GenericContainer(image)
        .withFileSystemBind(outputDir.absolutePath, "/app/gen", BindMode.READ_ONLY)
        .withCommand("tail", "-f", "/dev/null")

    container.start()

    return container to outputDir
}

fun Fixture.refinedTypeNames(): Set<String> {
    val ctx = object : ParseContext, NoLogger { override val spec = WirespecSpec }
    val ast = ctx.parse(nonEmptyListOf(ModuleContent(FileUri("N/A"), source)))
        .getOrNull() ?: return emptySet()
    return ast.modules.toList()
        .flatMap { it.statements.toList() }
        .filterIsInstance<Refined>()
        .map { it.identifier.value }
        .toSet()
}

/**
 * Adapts a canonical (non-TS) test file for TypeScript:
 * inlines refined wrappers, rewrites validate calls, and rebuilds imports.
 */
fun AstFile.adaptForTypeScript(fixture: Fixture): AstFile {
    val refinedTypes = fixture.refinedTypeNames()
    val main = elements.filterIsInstance<Main>().firstOrNull() ?: return this
    val body = main.body

    // Analyze: variable->type mapping and validate targets
    val variableTypes = mutableMapOf<String, String>()
    val validateTargets = mutableSetOf<String>()
    for (stmt in body) {
        if (stmt !is Assignment) continue
        when (val value = stmt.value) {
            is ConstructorStatement -> (value.type as? Type.Custom)?.let { variableTypes[stmt.name] = it.name }
            is FunctionCall -> if (value.name == "validate" && value.receiver is VariableReference)
                validateTargets.add((value.receiver as VariableReference).name)
            else -> {}
        }
    }

    // Refined wrappers to inline: refined type assignments NOT used as validate targets
    val inlineMap = body.filterIsInstance<Assignment>()
        .filter { variableTypes[it.name] in refinedTypes && it.name !in validateTargets }
        .mapNotNull { a -> (a.value as? ConstructorStatement)?.namedArguments?.get("value")?.let { a.name to it } }
        .toMap()

    // Transform: inline refs + rewrite validate calls
    val t = transformer(
        transformExpression = { expr, self ->
            when {
                expr is VariableReference && expr.name in inlineMap -> inlineMap.getValue(expr.name)
                expr is FunctionCall && expr.name == "validate" && expr.receiver is VariableReference -> {
                    val varName = (expr.receiver as VariableReference).name
                    val typeName = variableTypes[varName] ?: return@transformer expr.transformChildren(self)
                    val arg = if (typeName in refinedTypes) "value" to FieldCall(VariableReference(varName), "value")
                    else "obj" to VariableReference(varName)
                    FunctionCall(name = "validate$typeName", arguments = mapOf(arg))
                }
                else -> expr.transformChildren(self)
            }
        },
    )

    val transformedBody = body
        .filter { it !is Assignment || it.name !in inlineMap }
        .map { t.transformStatement(it) }

    // Rebuild imports: only validated types + their validate functions
    val newImports = validateTargets.flatMap { varName ->
        val typeName = variableTypes[varName] ?: return@flatMap emptyList()
        listOf(
            Import("./model/$typeName", Type.Custom(typeName)),
            Import("./model/$typeName", Type.Custom("validate$typeName")),
        )
    }.distinct()

    return copy(elements = newImports + elements.filter { it !is Import && it !is Main } + Main(transformedBody))
}

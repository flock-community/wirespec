package community.flock.wirespec.verify

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.ir.core.ContainerBuilder
import community.flock.wirespec.compiler.test.Fixture
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.AssertStatement
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Main
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.generator.generateJava
import community.flock.wirespec.ir.generator.generateKotlin
import community.flock.wirespec.ir.generator.generatePython
import community.flock.wirespec.ir.generator.generateRust
import community.flock.wirespec.ir.generator.generateScala
import community.flock.wirespec.ir.generator.generateTypeScript
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.io.File
import community.flock.wirespec.ir.core.File as AstFile

val languages = mapOf(
    "java-17" to Language(JavaIrEmitter(emitShared = EmitShared(true)), { "eclipse-temurin:17-jdk" }),
    "java-21" to Language(JavaIrEmitter(emitShared = EmitShared(true)), { "eclipse-temurin:21-jdk" }),
    "kotlin-1" to Language(KotlinIrEmitter(emitShared = EmitShared(true)), { VerifyImage.KOTLIN_1.image }),
    "kotlin-2" to Language(KotlinIrEmitter(emitShared = EmitShared(true)), { VerifyImage.KOTLIN_2.image }),
    "python" to Language(PythonIrEmitter(emitShared = EmitShared(true)), { VerifyImage.PYTHON.image }),
    "typescript" to Language(TypeScriptIrEmitter(), { "node:20-slim" }),
    "rust" to Language(RustIrEmitter(emitShared = EmitShared(true)), { VerifyImage.RUST.image }),
    "scala" to Language(ScalaIrEmitter(emitShared = EmitShared(true)), { VerifyImage.SCALA.image }),
).onEach { (name, lang) -> lang.name = name }

class Language(
    val emitter: IrEmitter,
    val image: () -> String,
) {
    lateinit var name: String
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
                    "moduleResolution": "node10",
                    "ignoreDeprecations": "6.0"
                  },
                  "include": ["./**/*.ts"]
                }
                """.trimIndent()
        )
    }

    fun generate(file: AstFile, outputDir: File) {
        val name = file.name.pascalCase()
        val (fileName, content) = when (emitter) {
            is JavaIrEmitter -> "${name}.java" to file.generateJava()
            is KotlinIrEmitter -> "${name}.kt" to file.generateKotlin()
            is PythonIrEmitter -> "${name}.py" to file.generatePython()
            is RustIrEmitter -> "${name}.rs" to file.generateRust()
            is ScalaIrEmitter -> "${name}.scala" to file.generateScala()
            is TypeScriptIrEmitter -> "${name}.ts" to file.generateTypeScript()
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
            is RustIrEmitter -> "rm -rf /app/src/generated && cp -r /app/gen/community/flock/wirespec/generated /app/src/generated && printf 'mod generated;\\nfn main() {}\\n' > /app/src/main.rs && cd /app && cargo build"
            is ScalaIrEmitter -> "find /app/gen -name '*.scala' | xargs scala-cli compile --server=false"
            is TypeScriptIrEmitter -> "npm install -g typescript && cd /app/gen && tsc --noEmit"
            else -> error("Unknown language: ${emitter::class.simpleName}")
        }
        exec(verifyCommand)
    }

    fun run(testFile: AstFile) {
        val resolved = if (emitter is TypeScriptIrEmitter) testFile.adaptForTypeScript(fixture) else testFile
        generate(resolved, outputDir)
        compile()
        val fileName = testFile.name.pascalCase()
        val runCommand: String = when (emitter) {
            is JavaIrEmitter -> "java -ea -cp /tmp/out $fileName"
            is KotlinIrEmitter -> "java -ea -cp /tmp/run.jar ${fileName}Kt"
            is PythonIrEmitter -> "cd /app/gen && python -O ${fileName}.py"
            is RustIrEmitter -> {
                // Build use statements from the test file's imports
                val imports = resolved.elements.filterIsInstance<Import>()
                val hasEndpointImports = imports.any { it.path.contains("endpoint") }
                val useStatements = imports.flatMap { imp ->
                    val typeName = imp.type.name
                    val snakeName = Name.of(typeName).snakeCase()
                    when {
                        imp.path.contains("endpoint") -> listOf(
                            "use generated::endpoint::${snakeName}::*;",
                            "use generated::endpoint::${snakeName}::${typeName}::*;",
                        )
                        imp.path.contains("model") -> listOf("use generated::model::${snakeName}::${typeName};")
                        else -> listOf("use generated::${snakeName}::${typeName};")
                    }
                }.joinToString("\n")
                // Import specific wirespec traits to avoid name clashes with endpoint types (Request, Response, etc.)
                val wirespecUse = if (hasEndpointImports) {
                    "use generated::wirespec::{BodySerializer, BodyDeserializer, PathSerializer, PathDeserializer, ParamSerializer, ParamDeserializer, BodySerialization, PathSerialization, ParamSerialization, Serializer, Deserializer, Serialization, RawRequest, RawResponse, Method};"
                } else ""
                // Generate the test file content (which contains fn main())
                val rustContent = resolved.generateRust()
                // Filter out the import lines that the generator produced (use super::...)
                val filteredContent = rustContent.lines()
                    .filter { !it.startsWith("use super::") }
                    .joinToString("\n")
                val mainRs = "mod generated;\n$useStatements\n$wirespecUse\n\n$filteredContent"
                container.execInContainer("sh", "-c", "cat > /app/src/main.rs << 'RUSTEOF'\n$mainRs\nRUSTEOF")
                "cd /app && cargo build && cargo run"
            }

            is ScalaIrEmitter -> "find /app/gen -name '*.scala' | xargs scala-cli run --server=false --main-class ${fileName}"
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
        if (result.exitCode != 0) {
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
    val ctx = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }
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
    val variableTypes = mutableMapOf<Name, String>()
    val validateTargets = mutableSetOf<Name>()
    for (stmt in body) {
        if (stmt !is Assignment) continue
        when (val value = stmt.value) {
            is ConstructorStatement -> (value.type as? Type.Custom)?.let { variableTypes[stmt.name] = it.name }
            is FunctionCall -> if (value.name == Name("validate") && value.receiver is VariableReference)
                validateTargets.add((value.receiver as VariableReference).name)

            else -> {}
        }
    }

    // Refined wrappers to inline: refined type assignments NOT used as validate targets
    val inlineMap = body.filterIsInstance<Assignment>()
        .filter { variableTypes[it.name] in refinedTypes && it.name !in validateTargets }
        .mapNotNull { a ->
            (a.value as? ConstructorStatement)?.namedArguments?.get(Name("value"))?.let { a.name to it }
        }
        .toMap()

    if (validateTargets.isEmpty() && inlineMap.isEmpty()) return this

    // Transform: inline refs + rewrite validate calls
    val t = transformer {
        expression { expr, self ->
            when {
                expr is VariableReference && expr.name in inlineMap -> inlineMap.getValue(expr.name)
                expr is FunctionCall && expr.name == Name("validate") && expr.receiver is VariableReference -> {
                    val varName = (expr.receiver as VariableReference).name
                    val typeName = variableTypes[varName] ?: return@expression expr.transformChildren(self)
                    val arg = if (typeName in refinedTypes) Name("value") to FieldCall(
                        VariableReference(varName),
                        Name("value")
                    )
                    else Name("obj") to VariableReference(varName)
                    FunctionCall(name = Name("validate$typeName"), arguments = mapOf(arg))
                }

                else -> expr.transformChildren(self)
            }
        }
    }

    // Second pass: wrap variable-to-variable equality in JSON.stringify for TypeScript
    // TypeScript uses === which compares references, not structural equality for arrays
    val jsonStringifyTransformer = transformer {
        statement { stmt, self ->
            if (stmt is AssertStatement && stmt.expression is BinaryOp) {
                val op = stmt.expression as BinaryOp
                if ((op.operator == BinaryOp.Operator.EQUALS || op.operator == BinaryOp.Operator.NOT_EQUALS) &&
                    op.left is VariableReference && op.right is VariableReference
                ) {
                    fun jsonStringify(e: Expression): FunctionCall = FunctionCall(
                        receiver = RawExpression("JSON"),
                        name = Name.of("stringify"),
                        arguments = mapOf(Name.of("_") to e),
                    )
                    AssertStatement(
                        BinaryOp(jsonStringify(op.left), op.operator, jsonStringify(op.right)),
                        stmt.message,
                    )
                } else {
                    stmt.transformChildren(self)
                }
            } else {
                stmt.transformChildren(self)
            }
        }
    }

    val transformedBody = body
        .filter { it !is Assignment || it.name !in inlineMap }
        .map { t.transformStatement(it) }
        .map { jsonStringifyTransformer.transformStatement(it) }

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

fun Fixture.definitions(): List<Definition> {
    val ctx = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }
    val ast = ctx.parse(nonEmptyListOf(ModuleContent(FileUri("N/A"), source)))
        .getOrNull() ?: return emptyList()
    return ast.modules.toList().flatMap { it.statements.toList() }
}

fun Fixture.endpointNames(): List<String> =
    definitions().filterIsInstance<Endpoint>().map { it.identifier.value }

fun Fixture.modelNames(): List<String> =
    definitions().filterIsInstance<Endpoint>()
        .flatMap { it.importReferences() }
        .distinctBy { it.value }
        .map { it.value }

fun ContainerBuilder.endpointClientImports(lang: Language, fixture: Fixture) {
    val endpoints = fixture.endpointNames()
    val models = fixture.modelNames()
    clientImportsShared(lang, endpoints, models)
    when (lang.emitter) {
        is JavaIrEmitter -> endpoints.forEach { import("community.flock.wirespec.generated.client", "${it}Client") }
        is KotlinIrEmitter -> endpoints.forEach { import("community.flock.wirespec.generated.client", "${it}Client") }
        is TypeScriptIrEmitter -> endpoints.forEach {
            val camel = Name.of(it).camelCase()
            import("./client/${it}Client", "${camel}Client")
        }
        is PythonIrEmitter -> endpoints.forEach {
            raw("from community.flock.wirespec.generated.client.${it}Client import ${it}Client")
        }
        is ScalaIrEmitter -> endpoints.forEach { import("community.flock.wirespec.generated.client", "${it}Client") }
        is RustIrEmitter -> {} // handled by run() use statements
    }
}

fun ContainerBuilder.mainClientImports(lang: Language, fixture: Fixture) {
    val endpoints = fixture.endpointNames()
    val models = fixture.modelNames()
    clientImportsShared(lang, endpoints, models)
    when (lang.emitter) {
        is JavaIrEmitter -> import("community.flock.wirespec.generated", "Client")
        is KotlinIrEmitter -> import("community.flock.wirespec.generated", "Client")
        is TypeScriptIrEmitter -> import("./Client", "client")
        is PythonIrEmitter -> raw("from community.flock.wirespec.generated.Client import Client")
        is ScalaIrEmitter -> import("community.flock.wirespec.generated", "Client")
        is RustIrEmitter -> {} // handled by run() use statements
    }
}

private fun ContainerBuilder.clientImportsShared(lang: Language, endpoints: List<String>, models: List<String>) {
    when (lang.emitter) {
        is JavaIrEmitter -> {
            import("community.flock.wirespec.java", "Wirespec")
            endpoints.forEach { import("community.flock.wirespec.generated.endpoint", it) }
            models.forEach { import("community.flock.wirespec.generated.model", it) }
        }
        is KotlinIrEmitter -> {
            import("community.flock.wirespec.kotlin", "Wirespec")
            endpoints.forEach { import("community.flock.wirespec.generated.endpoint", it) }
            models.forEach { import("community.flock.wirespec.generated.model", it) }
            import("kotlin.coroutines", "createCoroutine")
            import("kotlin.coroutines", "resume")
        }
        is TypeScriptIrEmitter -> {
            import("./Wirespec", "Wirespec")
            endpoints.forEach { import("./endpoint/$it", it) }
            models.forEach { import("./model/$it", it) }
        }
        is PythonIrEmitter -> {
            raw("from community.flock.wirespec.generated.wirespec import Wirespec")
            endpoints.forEach { raw("from community.flock.wirespec.generated.endpoint.$it import Response200") }
            models.forEach { raw("from community.flock.wirespec.generated.model.$it import $it") }
            raw("import asyncio")
        }
        is ScalaIrEmitter -> {
            import("community.flock.wirespec.scala", "Wirespec")
            endpoints.forEach { import("community.flock.wirespec.generated.endpoint", it) }
            models.forEach { import("community.flock.wirespec.generated.model", it) }
        }
        is RustIrEmitter -> {
            endpoints.forEach { import("community.flock.wirespec.generated.endpoint", it) }
            models.forEach { import("community.flock.wirespec.generated.model", it) }
        }
    }
}
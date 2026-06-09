package community.flock.wirespec.verify

import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.ContainerBuilder

/**
 * Imports for a test file that exercises the emitted test-data Generators.
 * Pulls in the shared Wirespec runtime plus a `<name>Generator` per given definition name.
 */
internal fun ContainerBuilder.generatorImports(lang: Language, definitionNames: List<String>) {
    when (lang.emitter) {
        is JavaIrEmitter -> {
            import("community.flock.wirespec.java", "Wirespec")
            definitionNames.forEach { import("community.flock.wirespec.generated.generator", "${it}Generator") }
        }
        is KotlinIrEmitter -> {
            import("community.flock.wirespec.kotlin", "Wirespec")
            definitionNames.forEach { import("community.flock.wirespec.generated.generator", "${it}Generator") }
        }
        is ScalaIrEmitter -> {
            import("community.flock.wirespec.scala", "Wirespec")
            definitionNames.forEach { import("community.flock.wirespec.generated.generator", "${it}Generator") }
        }
        is TypeScriptIrEmitter -> {
            import("./Wirespec", "Wirespec")
            definitionNames.forEach { import("./generator/${it}Generator", "${it}Generator") }
        }
        is PythonIrEmitter -> {
            import("community.flock.wirespec.generated.wirespec", "Wirespec")
            definitionNames.forEach { import("community.flock.wirespec.generated.generator.${it}Generator", "${it}Generator") }
        }
        else -> error("Generators are not verified for: ${lang.emitter::class.simpleName}")
    }
}

/**
 * A deterministic `Wirespec.Generator` implementation per language, bound to a `generator`
 * variable. Primitive fields get fixed values so tests can assert on the generated data:
 * strings are "string" ("1234AB" when a regex constraint is present, matching the
 * DutchPostalCode fixture), integers 42, numbers 4.2, booleans true. Composite fields
 * (shape, array, nullable, dict) recurse via the field's own generate callback.
 */
internal fun generatorCode(lang: Language): String = when (lang.emitter) {
    is JavaIrEmitter -> """
        |static Wirespec.Generator generator = new Wirespec.Generator() {
        |    @SuppressWarnings("unchecked")
        |    @Override
        |    public <T> T generate(java.util.List<String> path, Wirespec.GeneratorField<T> field) {
        |        if (field instanceof Wirespec.GeneratorFieldString f) return (T) (f.regex().isPresent() ? "1234AB" : "string");
        |        if (field instanceof Wirespec.GeneratorFieldInteger64 f) return (T) Long.valueOf(42L);
        |        if (field instanceof Wirespec.GeneratorFieldInteger32 f) return (T) Integer.valueOf(42);
        |        if (field instanceof Wirespec.GeneratorFieldNumber64 f) return (T) Double.valueOf(4.2);
        |        if (field instanceof Wirespec.GeneratorFieldNumber32 f) return (T) Float.valueOf(4.2f);
        |        if (field instanceof Wirespec.GeneratorFieldBoolean f) return (T) Boolean.TRUE;
        |        if (field instanceof Wirespec.GeneratorFieldBytes f) return (T) new byte[0];
        |        if (field instanceof Wirespec.GeneratorFieldEnum f) return (T) f.values().get(0);
        |        if (field instanceof Wirespec.GeneratorFieldUnion f) return (T) f.variants().get(0);
        |        if (field instanceof Wirespec.GeneratorFieldArray<?> f) return (T) java.util.List.of(f.generate().apply(append(path, "0")));
        |        if (field instanceof Wirespec.GeneratorFieldNullable<?> f) return (T) java.util.Optional.of(f.generate().apply(path));
        |        if (field instanceof Wirespec.GeneratorFieldShape<?> f) return (T) f.generate().apply(path);
        |        if (field instanceof Wirespec.GeneratorFieldDict<?> f) return (T) java.util.Map.of("key", f.generate().apply(append(path, "key")));
        |        throw new IllegalStateException("Unknown generator field: " + field);
        |    }
        |    private java.util.List<String> append(java.util.List<String> path, String segment) {
        |        return java.util.stream.Stream.concat(path.stream(), java.util.stream.Stream.of(segment)).toList();
        |    }
        |};
    """.trimMargin()

    is KotlinIrEmitter -> """
        |@Suppress("UNCHECKED_CAST")
        |val generator = object : Wirespec.Generator {
        |    override fun <T> generate(path: List<String>, field: Wirespec.GeneratorField<T>): T = when (field) {
        |        is Wirespec.GeneratorFieldString -> if (field.regex != null) "1234AB" else "string"
        |        is Wirespec.GeneratorFieldInteger64 -> 42L
        |        is Wirespec.GeneratorFieldInteger32 -> 42
        |        is Wirespec.GeneratorFieldNumber64 -> 4.2
        |        is Wirespec.GeneratorFieldNumber32 -> 4.2f
        |        is Wirespec.GeneratorFieldBoolean -> true
        |        is Wirespec.GeneratorFieldBytes -> ByteArray(0)
        |        is Wirespec.GeneratorFieldEnum -> field.values.first()
        |        is Wirespec.GeneratorFieldUnion -> field.variants.first()
        |        is Wirespec.GeneratorFieldArray<*> -> listOf(field.generate(path + "0"))
        |        is Wirespec.GeneratorFieldNullable<*> -> field.generate(path)
        |        is Wirespec.GeneratorFieldShape<*> -> field.generate(path)
        |        is Wirespec.GeneratorFieldDict<*> -> mapOf("key" to field.generate(path + "key"))
        |    } as T
        |}
    """.trimMargin()

    is TypeScriptIrEmitter -> """
        |const generator: Wirespec.Generator = {
        |    generate: <T>(path: string[], field: Wirespec.GeneratorField<T>): T => {
        |        const f = field as any;
        |        switch (f.kind) {
        |            case "string": return (f.regex !== undefined ? "1234AB" : "string") as unknown as T;
        |            case "integer64": return 42 as unknown as T;
        |            case "integer32": return 42 as unknown as T;
        |            case "number64": return 4.2 as unknown as T;
        |            case "number32": return 4.2 as unknown as T;
        |            case "boolean": return true as unknown as T;
        |            case "bytes": return new ArrayBuffer(0) as unknown as T;
        |            case "enum": return f.values[0] as unknown as T;
        |            case "union": return f.variants[0] as unknown as T;
        |            case "array": return [f.generate([...path, "0"])] as unknown as T;
        |            case "nullable": return f.generate(path) as unknown as T;
        |            case "shape": return f.generate(path) as unknown as T;
        |            case "dict": return { key: f.generate([...path, "key"]) } as unknown as T;
        |        }
        |        throw new Error("Unknown generator field: " + f.kind);
        |    }
        |};
    """.trimMargin()

    is PythonIrEmitter -> """
        |class TestGenerator(Wirespec.Generator):
        |    def generate(self, path, field):
        |        if isinstance(field, Wirespec.GeneratorFieldString):
        |            return "1234AB" if field.regex is not None else "string"
        |        if isinstance(field, (Wirespec.GeneratorFieldInteger64, Wirespec.GeneratorFieldInteger32)):
        |            return 42
        |        if isinstance(field, (Wirespec.GeneratorFieldNumber64, Wirespec.GeneratorFieldNumber32)):
        |            return 4.2
        |        if isinstance(field, Wirespec.GeneratorFieldBoolean):
        |            return True
        |        if isinstance(field, Wirespec.GeneratorFieldBytes):
        |            return b""
        |        if isinstance(field, Wirespec.GeneratorFieldEnum):
        |            return field.values[0]
        |        if isinstance(field, Wirespec.GeneratorFieldUnion):
        |            return field.variants[0]
        |        if isinstance(field, Wirespec.GeneratorFieldArray):
        |            return [field.generate(path + ["0"])]
        |        if isinstance(field, Wirespec.GeneratorFieldNullable):
        |            return field.generate(path)
        |        if isinstance(field, Wirespec.GeneratorFieldShape):
        |            return field.generate(path)
        |        if isinstance(field, Wirespec.GeneratorFieldDict):
        |            return {"key": field.generate(path + ["key"])}
        |        raise Exception("Unknown generator field")
        |generator = TestGenerator()
    """.trimMargin()

    is ScalaIrEmitter -> """
        |val generator: Wirespec.Generator = new Wirespec.Generator {
        |    def generate[T](path: List[String], field: Wirespec.GeneratorField[T]): T = (field match {
        |        case f: Wirespec.GeneratorFieldString => if (f.regex.isDefined) "1234AB" else "string"
        |        case f: Wirespec.GeneratorFieldInteger64 => 42L
        |        case f: Wirespec.GeneratorFieldInteger32 => 42
        |        case f: Wirespec.GeneratorFieldNumber64 => 4.2d
        |        case f: Wirespec.GeneratorFieldNumber32 => 4.2f
        |        case f: Wirespec.GeneratorFieldBoolean => true
        |        case f: Wirespec.GeneratorFieldBytes => Array.empty[Byte]
        |        case f: Wirespec.GeneratorFieldEnum => f.values.head
        |        case f: Wirespec.GeneratorFieldUnion => f.variants.head
        |        case f: Wirespec.GeneratorFieldArray[?] => List(f.generate(path :+ "0"))
        |        case f: Wirespec.GeneratorFieldNullable[?] => Option(f.generate(path))
        |        case f: Wirespec.GeneratorFieldShape[?] => f.generate(path)
        |        case f: Wirespec.GeneratorFieldDict[?] => Map("key" -> f.generate(path :+ "key"))
        |    }).asInstanceOf[T]
        |}
    """.trimMargin()

    else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
}

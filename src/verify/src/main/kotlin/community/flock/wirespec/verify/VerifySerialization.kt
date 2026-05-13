package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.parse.ast.Type as AstType
import community.flock.wirespec.compiler.test.Fixture
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter

internal fun serializationCode(lang: Language, fixture: Fixture? = null): String = when (lang.emitter) {
    is JavaIrEmitter -> """
        |static Wirespec.Serialization serialization = new Wirespec.Serialization() {
        |    private final java.util.Map<String, Object> store = new java.util.HashMap<>();
        |    private String randomKey() { return java.util.UUID.randomUUID().toString(); }
        |    @Override public <T> byte[] serializeBody(T t, java.lang.reflect.Type type) { String key = randomKey(); store.put(key, t); return key.getBytes(); }
        |    @Override public <T> T deserializeBody(byte[] raw, java.lang.reflect.Type type) { return (T) store.get(new String(raw)); }
        |    @Override public <T> String serializePath(T t, java.lang.reflect.Type type) { return t.toString(); }
        |    @Override public <T> T deserializePath(String raw, java.lang.reflect.Type type) { return (T) raw; }
        |    @Override public <T> java.util.List<String> serializeParam(T value, java.lang.reflect.Type type) { return java.util.List.of(value.toString()); }
        |    @Override public <T> T deserializeParam(java.util.List<String> values, java.lang.reflect.Type type) {
        |        String v = values.get(0);
        |        Class<?> cls = type instanceof Class ? (Class<?>) type : (Class<?>) ((java.lang.reflect.ParameterizedType) type).getRawType();
        |        if (cls == Boolean.class || cls == boolean.class) return (T) Boolean.valueOf(v);
        |        if (cls == Integer.class || cls == int.class) return (T) Integer.valueOf(v);
        |        if (cls == String.class) return (T) v;
        |        try { return (T) cls.getConstructors()[0].newInstance(v); } catch (Exception e) { return (T) v; }
        |    }
        |};
    """.trimMargin()

    is KotlinIrEmitter -> """
        |val serialization = object : Wirespec.Serialization {
        |    private val store = mutableMapOf<String, Any>()
        |    private fun randomKey() = java.util.UUID.randomUUID().toString()
        |    override fun <T : Any> serializeBody(t: T, kType: kotlin.reflect.KType): ByteArray { val key = randomKey(); store[key] = t; return key.toByteArray() }
        |    override fun <T : Any> deserializeBody(raw: ByteArray, kType: kotlin.reflect.KType): T = store[String(raw)] as T
        |    override fun <T : Any> serializePath(t: T, kType: kotlin.reflect.KType): String = t.toString()
        |    override fun <T : Any> deserializePath(raw: String, kType: kotlin.reflect.KType): T = raw as T
        |    override fun <T : Any> serializeParam(value: T, kType: kotlin.reflect.KType): List<String> = listOf(value.toString())
        |    override fun <T : Any> deserializeParam(values: List<String>, kType: kotlin.reflect.KType): T {
        |        val v = values[0]
        |        val cls = kType.classifier as? kotlin.reflect.KClass<*>
        |        if (cls == Boolean::class) return v.toBoolean() as T
        |        if (cls == Int::class) return v.toInt() as T
        |        if (cls == String::class) return v as T
        |        return cls!!.constructors.first().call(v) as T
        |    }
        |}
    """.trimMargin()

    is TypeScriptIrEmitter -> """
        |const store: Record<string, unknown> = {};
        |let counter = 0;
        |const serialization: Wirespec.Serialization = {
        |    serializeBody: <T>(t: T, type: Wirespec.Type): Uint8Array => { const key = String(counter++); store[key] = t; return new TextEncoder().encode(key); },
        |    deserializeBody: <T>(raw: Uint8Array, type: Wirespec.Type): T => store[new TextDecoder().decode(raw)] as T,
        |    serializePath: <T>(t: T, type: Wirespec.Type): string => String(t),
        |    deserializePath: <T>(raw: string, type: Wirespec.Type): T => raw as unknown as T,
        |    serializeParam: <T>(value: T, type: Wirespec.Type): string[] => [String(value)],
        |    deserializeParam: <T>(values: string[], type: Wirespec.Type): T => {
        |        const v = values[0];
        |        if (type === "boolean") return (v === "true") as unknown as T;
        |        if (type === "number") return Number(v) as unknown as T;
        |        if (type === "string") return v as unknown as T;
        |        return ({ iss: v }) as unknown as T;
        |    },
        |}
    """.trimMargin()

    is PythonIrEmitter -> """
        |class TestSerialization(Wirespec.Serialization):
        |    def __init__(self):
        |        self.store = {}
        |        self.counter = 0
        |    def serializeBody(self, t, type):
        |        key = str(self.counter)
        |        self.counter += 1
        |        self.store[key] = t
        |        return key.encode()
        |    def deserializeBody(self, raw, type):
        |        return self.store[raw.decode()]
        |    def serializePath(self, t, type):
        |        return str(t)
        |    def deserializePath(self, raw, type):
        |        return raw
        |    def serializeParam(self, value, type):
        |        return [str(value)]
        |    def deserializeParam(self, values, type):
        |        v = values[0]
        |        if type == bool: return v.lower() == 'true'
        |        if type == int: return int(v)
        |        if type == str: return v
        |        return type(v)
        |serialization = TestSerialization()
    """.trimMargin()

    is RustIrEmitter -> rustSerializationCode(fixture)

    is ScalaIrEmitter -> """
        |val serialization = new Wirespec.Serialization {
        |    private val store = scala.collection.mutable.Map[String, Any]()
        |    private def randomKey(): String = java.util.UUID.randomUUID().toString
        |    override def serializeBody[T](t: T, classTag: scala.reflect.ClassTag[?]): Array[Byte] = { val key = randomKey(); store(key) = t; key.getBytes }
        |    override def deserializeBody[T](raw: Array[Byte], classTag: scala.reflect.ClassTag[?]): T = store(new String(raw)).asInstanceOf[T]
        |    override def serializePath[T](t: T, classTag: scala.reflect.ClassTag[?]): String = t.toString
        |    override def deserializePath[T](raw: String, classTag: scala.reflect.ClassTag[?]): T = raw.asInstanceOf[T]
        |    override def serializeParam[T](value: T, classTag: scala.reflect.ClassTag[?]): List[String] = List(value.toString)
        |    override def deserializeParam[T](values: List[String], classTag: scala.reflect.ClassTag[?]): T = {
        |        val v = values.head
        |        val cls = classTag.runtimeClass
        |        if (cls == classOf[Boolean]) java.lang.Boolean.parseBoolean(v).asInstanceOf[T]
        |        else if (cls == classOf[Int]) v.toInt.asInstanceOf[T]
        |        else if (cls == classOf[String]) v.asInstanceOf[T]
        |        else cls.getConstructors.head.newInstance(v).asInstanceOf[T]
        |    }
        |}
    """.trimMargin()

    else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
}

private fun rustSerializationCode(fixture: Fixture?): String {
    val types = fixture?.definitions()?.filterIsInstance<AstType>()?.associate {
        it.identifier.value to it.shape.value.map { f -> f.identifier.value to f.reference }
    } ?: emptyMap()

    fun serializeBranch(typeName: String, fields: List<Pair<String, *>>): String {
        val jsonFields = fields.joinToString(", ") { (name, _) -> "\"$name\": v.$name" }
        return "if let Some(v) = any.downcast_ref::<$typeName>() {\n        |            serde_json::to_vec(&serde_json::json!({$jsonFields})).unwrap()"
    }

    fun serializeVecBranch(typeName: String, fields: List<Pair<String, *>>): String {
        val jsonFields = fields.joinToString(", ") { (name, _) -> "\"$name\": td.$name" }
        return "if let Some(v) = any.downcast_ref::<Vec<$typeName>>() {\n        |            serde_json::to_vec(&v.iter().map(|td| serde_json::json!({$jsonFields})).collect::<Vec<serde_json::Value>>()).unwrap()"
    }

    fun deserializeBranch(typeName: String, fields: List<Pair<String, *>>): String {
        val fieldInits = fields.joinToString(", ") { (name, ref) ->
            val accessor = when {
                ref.toString().contains("Boolean") -> "v[\"$name\"].as_bool().unwrap_or_default()"
                ref.toString().contains("Integer") -> "v[\"$name\"].as_i64().unwrap_or_default() as i64"
                else -> "v[\"$name\"].as_str().unwrap_or_default().to_string()"
            }
            "$name: $accessor"
        }
        return "if _type == std::any::TypeId::of::<$typeName>() {\n        |            let v: serde_json::Value = serde_json::from_slice(raw).unwrap();\n        |            Box::new($typeName { $fieldInits })"
    }

    fun deserializeVecBranch(typeName: String, fields: List<Pair<String, *>>): String {
        val fieldInits = fields.joinToString(", ") { (name, ref) ->
            val accessor = when {
                ref.toString().contains("Boolean") -> "v[\"$name\"].as_bool().unwrap_or_default()"
                ref.toString().contains("Integer") -> "v[\"$name\"].as_i64().unwrap_or_default() as i64"
                else -> "v[\"$name\"].as_str().unwrap_or_default().to_string()"
            }
            "$name: $accessor"
        }
        return "if _type == std::any::TypeId::of::<Vec<$typeName>>() {\n        |            let values: Vec<serde_json::Value> = serde_json::from_slice(raw).unwrap();\n        |            let items: Vec<$typeName> = values.iter().map(|v| $typeName { $fieldInits }).collect();\n        |            Box::new(items)"
    }

    // Build body serializer branches
    val serBranches = types.flatMap { (name, fields) ->
        listOf(serializeVecBranch(name, fields), serializeBranch(name, fields))
    }
    val serBody = if (serBranches.isEmpty()) {
        "panic!(\"Unsupported body type for serialization: {:?}\", _type)"
    } else {
        serBranches.joinToString("\n        |        } else ") { it } + "\n        |        } else {\n        |            panic!(\"Unsupported body type for serialization: {:?}\", _type)\n        |        }"
    }

    // Build body deserializer branches
    val deserBranches = types.flatMap { (name, fields) ->
        listOf(deserializeVecBranch(name, fields), deserializeBranch(name, fields))
    }
    val deserBody = if (deserBranches.isEmpty()) {
        "panic!(\"Unsupported body type for deserialization: {:?}\", _type)"
    } else {
        "let boxed: Box<dyn std::any::Any> = " + deserBranches.joinToString("\n        |        } else ") { it } + "\n        |        } else {\n        |            panic!(\"Unsupported body type for deserialization: {:?}\", _type)\n        |        };\n        |        *boxed.downcast::<T>().unwrap()"
    }

    // Build param deserializer branches for custom types
    val paramBranches = types.filter { (_, fields) -> fields.size == 1 }.map { (name, fields) ->
        val fieldName = fields.first().first
        "} else if _type == std::any::TypeId::of::<$name>() {\n        |            Box::new($name { $fieldName: values.first().cloned().unwrap_or_default() })"
    }
    val paramExtra = paramBranches.joinToString("\n        |        ")

    return """
        |struct MockSer;
        |impl BodySerializer for MockSer {
        |    fn serialize_body<T: 'static>(&self, t: &T, _type: std::any::TypeId) -> Vec<u8> {
        |        let any: &dyn std::any::Any = t;
        |        $serBody
        |    }
        |}
        |impl BodyDeserializer for MockSer {
        |    fn deserialize_body<T: 'static>(&self, raw: &[u8], _type: std::any::TypeId) -> T {
        |        $deserBody
        |    }
        |}
        |impl PathSerializer for MockSer {
        |    fn serialize_path<T: std::fmt::Display>(&self, t: &T, _type: std::any::TypeId) -> String { t.to_string() }
        |}
        |impl PathDeserializer for MockSer {
        |    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, _type: std::any::TypeId) -> T where T::Err: std::fmt::Debug { raw.parse().unwrap() }
        |}
        |impl ParamSerializer for MockSer {
        |    fn serialize_param<T: 'static>(&self, value: &T, _type: std::any::TypeId) -> Vec<String> {
        |        let any: &dyn std::any::Any = value;
        |        if let Some(s) = any.downcast_ref::<String>() { vec![s.clone()] }
        |        else if let Some(b) = any.downcast_ref::<bool>() { vec![b.to_string()] }
        |        else { panic!("Unsupported param type for serialization: {:?}", _type) }
        |    }
        |}
        |impl ParamDeserializer for MockSer {
        |    fn deserialize_param<T: 'static>(&self, values: &[String], _type: std::any::TypeId) -> T {
        |        let boxed: Box<dyn std::any::Any> = if _type == std::any::TypeId::of::<String>() {
        |            Box::new(values.first().cloned().unwrap_or_default())
        |        } else if _type == std::any::TypeId::of::<bool>() {
        |            Box::new(values.first().map(|v| v == "true").unwrap_or(false))
        |        $paramExtra
        |        } else {
        |            panic!("Unsupported param type for deserialization: {:?}", _type)
        |        };
        |        *boxed.downcast::<T>().unwrap()
        |    }
        |}
        |impl BodySerialization for MockSer {}
        |impl PathSerialization for MockSer {}
        |impl ParamSerialization for MockSer {}
        |impl Serializer for MockSer {}
        |impl Deserializer for MockSer {}
        |impl Serialization for MockSer {}
        |#[allow(non_upper_case_globals)]
        |static serialization: MockSer = MockSer;
    """.trimMargin()
}

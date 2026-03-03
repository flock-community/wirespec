package community.flock.wirespec.verify

import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter

internal fun serializationCode(lang: Language): String = when (lang.emitter) {
    is JavaIrEmitter -> """
        |Wirespec.Serialization serialization = new Wirespec.Serialization() {
        |    private final java.util.Map<String, Object> store = new java.util.HashMap<>();
        |    private String randomKey() { return java.util.UUID.randomUUID().toString(); }
        |    @Override public <T> byte[] serializeBody(T t, java.lang.reflect.Type type) { String key = randomKey(); store.put(key, t); return key.getBytes(); }
        |    @Override public <T> T deserializeBody(byte[] raw, java.lang.reflect.Type type) { return (T) store.get(new String(raw)); }
        |    @Override public <T> String serializePath(T t, java.lang.reflect.Type type) { return t.toString(); }
        |    @Override public <T> T deserializePath(String raw, java.lang.reflect.Type type) { return (T) raw; }
        |    @Override public <T> java.util.List<String> serializeParam(T value, java.lang.reflect.Type type) { return java.util.List.of(value.toString()); }
        |    @Override public <T> T deserializeParam(java.util.List<String> values, java.lang.reflect.Type type) { return (T) values.get(0); }
        |}
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
        |    override fun <T : Any> deserializeParam(values: List<String>, kType: kotlin.reflect.KType): T = values[0] as T
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
        |    deserializeParam: <T>(values: string[], type: Wirespec.Type): T => values[0] as unknown as T,
        |}
    """.trimMargin()

    is PythonIrEmitter -> "serialization = TestSerialization()"

    is RustIrEmitter -> "let serialization = MockSer"

    is ScalaIrEmitter -> """
        |val serialization = new Wirespec.Serialization {
        |    private val store = scala.collection.mutable.Map[String, Any]()
        |    private def randomKey(): String = java.util.UUID.randomUUID().toString
        |    override def serializeBody[T](t: T, classTag: scala.reflect.ClassTag[?]): Array[Byte] = { val key = randomKey(); store(key) = t; key.getBytes }
        |    override def deserializeBody[T](raw: Array[Byte], classTag: scala.reflect.ClassTag[?]): T = store(new String(raw)).asInstanceOf[T]
        |    override def serializePath[T](t: T, classTag: scala.reflect.ClassTag[?]): String = t.toString
        |    override def deserializePath[T](raw: String, classTag: scala.reflect.ClassTag[?]): T = raw.asInstanceOf[T]
        |    override def serializeParam[T](value: T, classTag: scala.reflect.ClassTag[?]): List[String] = List(value.toString)
        |    override def deserializeParam[T](values: List[String], classTag: scala.reflect.ClassTag[?]): T = values.head.asInstanceOf[T]
        |}
    """.trimMargin()

    else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
}

internal fun pythonSerializationDefs(): String = """
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
    |        return values[0]
""".trimMargin()

internal fun rustSerializationDefs(): String = """
    |struct MockSer;
    |impl BodySerializer for MockSer {
    |    fn serialize_body<T: 'static>(&self, t: &T, _type: std::any::TypeId) -> Vec<u8> {
    |        let any: &dyn std::any::Any = t;
    |        if let Some(v) = any.downcast_ref::<Vec<TodoDto>>() {
    |            serde_json::to_vec(&v.iter().map(|td| serde_json::json!({"description": td.description})).collect::<Vec<serde_json::Value>>()).unwrap()
    |        } else {
    |            panic!("Unsupported body type for serialization: {:?}", _type)
    |        }
    |    }
    |}
    |impl BodyDeserializer for MockSer {
    |    fn deserialize_body<T: 'static>(&self, raw: &[u8], _type: std::any::TypeId) -> T {
    |        let boxed: Box<dyn std::any::Any> = if _type == std::any::TypeId::of::<Vec<TodoDto>>() {
    |            let values: Vec<serde_json::Value> = serde_json::from_slice(raw).unwrap();
    |            let todos: Vec<TodoDto> = values.iter().map(|v| TodoDto { description: v["description"].as_str().unwrap_or_default().to_string() }).collect();
    |            Box::new(todos)
    |        } else {
    |            panic!("Unsupported body type for deserialization: {:?}", _type)
    |        };
    |        *boxed.downcast::<T>().unwrap()
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
""".trimMargin()

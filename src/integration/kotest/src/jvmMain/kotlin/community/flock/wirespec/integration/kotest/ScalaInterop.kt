package community.flock.wirespec.integration.kotest

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

/**
 * Reflective Scala ↔ Kotlin conversions used by [WirespecScalaGeneratorAdapter].
 *
 * Scala-stdlib module handles (`scala.Option$`, `scala.jdk.javaapi
 * .CollectionConverters$`, …) and their hot accessor methods are cached in
 * `lazy` properties so frequent paths pay only `Method.invoke` cost. Less
 * common method lookups (e.g. `toList` on a converted Scala iterable,
 * `apply` on a runtime `Function1` lambda) are resolved per-call against
 * the actual receiver class. The adapter has no compile-time dependency on
 * Scala — every Scala type is named by string FQN here.
 */
internal object ScalaInterop {

    // --- Scala-stdlib reflective handles, all lazy --------------------------

    private val cl: ClassLoader by lazy {
        Thread.currentThread().contextClassLoader ?: javaClass.classLoader
    }

    private val optionModule: Any by lazy {
        cl.loadClass("scala.Option\$").getField("MODULE\$").get(null)
    }
    private val optionApply: Method by lazy {
        optionModule.javaClass.getMethod("apply", Any::class.java)
    }
    private val optionEmpty: Method by lazy {
        optionModule.javaClass.getMethod("empty")
    }

    private val convertersModule: Any by lazy {
        cl.loadClass("scala.jdk.javaapi.CollectionConverters\$").getField("MODULE\$").get(null)
    }
    private val convertersAsJava: Method by lazy {
        convertersModule.javaClass.getMethod("asJava", cl.loadClass("scala.collection.Iterable"))
    }
    private val convertersAsJavaMap: Method by lazy {
        convertersModule.javaClass.getMethod("asJava", cl.loadClass("scala.collection.Map"))
    }
    private val convertersAsScala: Method by lazy {
        convertersModule.javaClass.getMethod("asScala", java.lang.Iterable::class.java)
    }
    private val convertersAsScalaMap: Method by lazy {
        convertersModule.javaClass.getMethod("asScala", java.util.Map::class.java)
    }

    private val optionIsEmpty: Method by lazy {
        cl.loadClass("scala.Option").getMethod("isEmpty")
    }
    private val optionGet: Method by lazy {
        cl.loadClass("scala.Option").getMethod("get")
    }

    private val scalaMapClass: Class<*> by lazy {
        cl.loadClass("scala.collection.Map")
    }

    // Per-receiver-class lookups (`apply` on runtime Function1 lambdas,
    // `toList` on converted iterables) are on the hot per-field path; cache
    // them so repeated generation pays only Method.invoke.
    private val applyMethods = ConcurrentHashMap<Class<*>, Method>()
    private val toListMethods = ConcurrentHashMap<Class<*>, Method>()

    // --- Public dispatch ----------------------------------------------------

    fun dispatch(inner: KotestGenerator, method: Method, args: Array<Any?>): Any? {
        check(method.name == "generate") {
            "Scala adapter received unexpected method call: ${method.name}"
        }
        val scalaPath = args[0]!!
        val scalaField = args[1]!!
        val kotlinPath = scalaListToKotlin(scalaPath)
        val kotestField = scalaFieldToKotest(scalaField)
        val resultKotlin = inner.generate(kotlinPath, kotestField)
        return adaptResultForScala(resultKotlin, scalaField)
    }

    // --- Conversions --------------------------------------------------------

    /** scala.collection.immutable.List<String> → kotlin List<String>. */
    @Suppress("UNCHECKED_CAST")
    fun scalaListToKotlin(scalaList: Any): List<String> {
        val asJavaIterable = convertersAsJava.invoke(convertersModule, scalaList) as Iterable<*>
        return asJavaIterable.map { it as String }
    }

    /** kotlin List<X> → scala.collection.immutable.List<X>. */
    private fun kotlinListToScala(xs: List<*>): Any {
        val javaList = ArrayList<Any?>(xs)
        val scalaIterable = convertersAsScala.invoke(convertersModule, javaList)
        val toList = toListMethods.getOrPut(scalaIterable.javaClass) { scalaIterable.javaClass.getMethod("toList") }
        return toList.invoke(scalaIterable)
    }

    /** kotlin Map<String, V> → scala.collection.immutable.Map<String, V>. */
    private fun kotlinMapToScala(m: Map<*, *>): Any {
        val javaMap = HashMap<Any?, Any?>(m)
        val scalaMutableMap = convertersAsScalaMap.invoke(convertersModule, javaMap)
        val toMap = scalaMutableMap.javaClass.getMethod("toMap", cl.loadClass("scala.\$less\$colon\$less"))
        val lessColonLessModule = cl.loadClass("scala.\$less\$colon\$less\$").getField("MODULE\$").get(null)
        val refl = lessColonLessModule.javaClass.getMethod("refl").invoke(lessColonLessModule)
        return toMap.invoke(scalaMutableMap, refl)
    }

    /** scala.Option<T> → kotlin T?, or returns the value if it isn't an Option. */
    private fun scalaOptionToNullable(option: Any?): Any? {
        if (option == null) return null
        if (optionIsEmpty.invoke(option) as Boolean) return null
        return optionGet.invoke(option)
    }

    /** kotlin T? → scala.Option<T>. */
    private fun nullableToScalaOption(value: Any?): Any = if (value == null) optionEmpty.invoke(optionModule) else optionApply.invoke(optionModule, value)

    /**
     * Decode a Scala `Wirespec$GeneratorField*` case-class instance into the
     * commonMain `KotestField*` mirror by case-class accessor name. Each
     * Scala case class generates a `<componentName>()` accessor matching its
     * constructor parameter — the same convention Java records use.
     */
    @Suppress("UNCHECKED_CAST")
    fun scalaFieldToKotest(field: Any): KotestField<Any?> {
        val cls = field.javaClass
        val simple = cls.simpleName
        return when (simple) {
            "GeneratorFieldString" -> {
                val regex = scalaOptionToNullable(cls.getMethod("regex").invoke(field)) as String?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldString(regex, annotations) as KotestField<Any?>
            }
            "GeneratorFieldInteger64" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Long?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Long?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldInteger64(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldInteger32" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Int?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Int?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldInteger32(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldNumber64" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Double?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Double?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldNumber64(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldNumber32" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Float?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Float?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldNumber32(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldBoolean" -> KotestFieldBoolean(decodeAnnotations(cls.getMethod("annotations").invoke(field))) as KotestField<Any?>
            "GeneratorFieldBytes" -> KotestFieldBytes(decodeAnnotations(cls.getMethod("annotations").invoke(field))) as KotestField<Any?>
            "GeneratorFieldEnum" -> {
                val values = scalaListToKotlin(cls.getMethod("values").invoke(field))
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldEnum(values, annotations, classTagToKType(field)) as KotestField<Any?>
            }
            "GeneratorFieldUnion" -> {
                val variants = scalaListToKotlin(cls.getMethod("variants").invoke(field))
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldUnion(variants, annotations, classTagToKType(field)) as KotestField<Any?>
            }
            "GeneratorFieldArray" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldArray<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            "GeneratorFieldNullable" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldNullable<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            "GeneratorFieldShape" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                val scalaAnnotations = cls.getMethod("annotations").invoke(field)
                val annotations = decodeShapeAnnotations(scalaAnnotations)
                KotestFieldShape<Any>(annotations, { p -> invokeScalaFunction1(scalaFn, p) }, classTagToKType(field)) as KotestField<Any?>
            }
            "GeneratorFieldDict" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldDict<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            else -> error("Unrecognised Scala GeneratorField: $simple")
        }
    }

    /**
     * Re-wrap the kotest result in the shape expected by the originating
     * Scala field type:
     *   - GeneratorFieldNullable   → result wrapped in scala.Option
     *   - GeneratorFieldArray      → result (a kotlin List) wrapped in scala.collection.immutable.List
     *   - GeneratorFieldDict       → result (a kotlin Map) wrapped in scala.collection.immutable.Map
     *   - everything else          → returned as-is (primitives, Strings, byte[])
     */
    fun adaptResultForScala(result: Any?, originalField: Any): Any? = when (originalField.javaClass.simpleName) {
        "GeneratorFieldNullable" -> nullableToScalaOption(result)
        "GeneratorFieldArray" -> kotlinListToScala(result as List<*>)
        "GeneratorFieldDict" -> kotlinMapToScala(result as Map<*, *>)
        else -> result
    }

    // --- Helpers ------------------------------------------------------------

    /**
     * Extract the `type: scala.reflect.ClassTag[T]` component of a Scala
     * `GeneratorField*` case class and convert its `runtimeClass` to a `KType`,
     * so field overrides by parent type and the Refined auto-wrap work the
     * same as for Kotlin-emitted code. Falls back to `typeOf<Any>()` when the
     * component is missing or not a ClassTag.
     */
    private fun classTagToKType(field: Any): KType = runCatching {
        val tag = field.javaClass.getMethod("type").invoke(field)!!
        val runtimeClass = tag.javaClass.getMethod("runtimeClass").invoke(tag) as Class<*>
        runtimeClass.kotlin.starProjectedType
    }.getOrDefault(typeOf<Any>())

    /** scala.Function1.apply(kotlinList) — wraps the kotlin list back as Scala first. */
    private fun invokeScalaFunction1(scalaFn: Any, kotlinPath: List<String>): Any {
        val scalaArg = kotlinListToScala(kotlinPath)
        val applyMethod = applyMethods.getOrPut(scalaFn.javaClass) {
            scalaFn.javaClass.getMethod("apply", Any::class.java).also { it.isAccessible = true }
        }
        return applyMethod.invoke(scalaFn, scalaArg)
    }

    /** Convert `scala.collection.immutable.List<scala.collection.immutable.Map<String,Any>>` → kotlin equivalent. */
    @Suppress("UNCHECKED_CAST")
    private fun decodeAnnotations(scalaAnnotations: Any?): List<Map<String, Any>> {
        if (scalaAnnotations == null) return emptyList()
        val asJavaIterable = convertersAsJava.invoke(convertersModule, scalaAnnotations) as Iterable<*>
        return asJavaIterable.map { scalaMap -> decodeScalaMap(scalaMap!!) }
    }

    /**
     * Convert Shape annotations (`Map[String, List[Map[String,Any]]]`) — a
     * Map whose values are themselves Lists of Maps. Both layers go through
     * CollectionConverters; nested Map values are recursively converted by
     * `decodeAnnotations`.
     */
    @Suppress("UNCHECKED_CAST")
    private fun decodeShapeAnnotations(scalaShape: Any?): Map<String, List<Map<String, Any>>> {
        if (scalaShape == null) return emptyMap()
        val asJavaMap = convertersAsJavaMap.invoke(convertersModule, scalaShape) as MutableMap<String, Any>
        return asJavaMap.mapValues { (_, scalaList) -> decodeAnnotations(scalaList) }
    }

    /** scala.collection.immutable.Map<String,Any> → kotlin Map<String,Any>. Nested Maps are recursively converted. */
    @Suppress("UNCHECKED_CAST")
    private fun decodeScalaMap(scalaMap: Any): Map<String, Any> {
        val asJava = convertersAsJavaMap.invoke(convertersModule, scalaMap) as MutableMap<String, Any>
        return asJava.mapValues { (_, v) ->
            // isInstance, not a class-name prefix check: small maps are
            // Map$.MapN but 5+ entries become immutable.HashMap.
            if (scalaMapClass.isInstance(v)) decodeScalaMap(v) else v
        }
    }
}

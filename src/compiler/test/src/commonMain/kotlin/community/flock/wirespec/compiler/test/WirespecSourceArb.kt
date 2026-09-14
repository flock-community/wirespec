package community.flock.wirespec.compiler.test

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.random.Random
import kotlin.test.fail

/**
 * Which slice of the grammar a generated source may use.
 *
 * The Wirespec emitter cannot reproduce everything the parser accepts, so round-trip
 * properties have to generate a narrower language than tokenizer and parser properties do.
 * Every flag that [WirespecFeatures.roundTrippable] turns off is a known emitter gap, not a parser limit.
 */
public data class WirespecFeatures(
    val annotations: Boolean,
    val comments: Boolean,
    val constrainedReferences: Boolean,
) {
    public companion object {
        /** Everything the parser accepts. */
        public val all: WirespecFeatures = WirespecFeatures(
            annotations = true,
            comments = true,
            constrainedReferences = true,
        )

        /** The subset `WirespecEmitter` reproduces verbatim. */
        public val roundTrippable: WirespecFeatures = WirespecFeatures(
            annotations = false,
            comments = false,
            constrainedReferences = false,
        )
    }
}

/** Generators of Wirespec source text, for property-based tokenizer, parser and round-trip tests. */
public object WirespecSourceArb {

    /** Source that the parser is expected to accept. */
    public fun source(features: WirespecFeatures = WirespecFeatures.all): Arb<String> = arbitrary { rs -> rs.random.module(features) }

    /** Text the parser is expected to survive — random, truncated and mutated input. */
    public fun noise(): Arb<String> = arbitrary { rs -> rs.random.noise() }
}

private val NOUNS = listOf("Todo", "User", "Account", "Order", "Invoice", "Device", "Session", "Payload", "Tag", "Address")
private val WORDS = listOf("dev", "test", "1.0.0", "TagA", "TagB", "prod")
private val KEYS = listOf("min", "max", "env", "debug", "roles", "tags", "window")
private val ANNOTATIONS = listOf("Deprecated", "Internal", "Since", "Validate", "Config", "Tag", "Security", "Experimental")
private val COMMENTS = listOf("a note", "see the spec", "kept for compatibility")
private val METHODS = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE")
private val STATUSES = listOf("200", "201", "400", "404", "500")
private val STRING_CONSTRAINTS = listOf("(/[a-z]+/)", "(/^[A-Z]{2}$/g)", "(/.*/)", "(/[0-9]{4}[A-Z]{2}/)")
private val INTEGER_BOUNDS = listOf("(1,5)", "(_,10)", "(0,_)", "(_,_)", "(-5,5)")
private val NUMBER_BOUNDS = listOf("(0.1,5.0)", "(_,0.5)", "(-0.2,0.5)", "(_,_)")

private enum class ModelKind { TYPE, REFINED, ENUM, UNION }

private enum class OtherKind { CHANNEL, RPC, ENDPOINT }

private fun <T> Random.pick(options: List<T>): T = options[nextInt(options.size)]

private fun Random.module(features: WirespecFeatures): String {
    val models = List(nextInt(2, 6)) { "${pick(NOUNS)}$it" }
    // The first two models are never unions, so a union always has defined entries to point at.
    val kinds = models.indices.map { if (it < 2) pick(listOf(ModelKind.TYPE, ModelKind.REFINED, ModelKind.ENUM)) else pick(ModelKind.entries) }
    val modelDefinitions = models.mapIndexed { index, name -> model(kinds[index], name, models, features) }
    val otherDefinitions = List(nextInt(0, 4)) { index -> other(pick(OtherKind.entries), "${pick(NOUNS)}Api$index", models, features) }
    return (modelDefinitions + otherDefinitions).joinToString("\n")
}

private fun Random.model(kind: ModelKind, name: String, models: List<String>, features: WirespecFeatures): String = metadata(features) + when (kind) {
    ModelKind.TYPE -> "type $name {\n${shape(models, features)}\n}\n"
    ModelKind.REFINED -> "type $name = ${refinedReference()}\n"
    ModelKind.ENUM -> "enum $name {\n  ${enumEntries().joinToString(", ")}\n}\n"
    ModelKind.UNION -> "type $name = ${unionEntries(models, name).joinToString(" | ")}\n"
}

private fun Random.other(kind: OtherKind, name: String, models: List<String>, features: WirespecFeatures): String = metadata(features) + when (kind) {
    OtherKind.CHANNEL -> "channel $name -> ${reference(models, features)}\n"
    OtherKind.RPC -> rpc(name, models, features)
    OtherKind.ENDPOINT -> endpoint(name, models, features)
}

private fun Random.rpc(name: String, models: List<String>, features: WirespecFeatures): String {
    val shape = if (nextInt(4) == 0) "{}" else "{\n${shape(models, features)}\n}"
    // `RpcParser.parseReference` only accepts a `WirespecType`, so an rpc result or error
    // cannot be a dict — unlike a channel reference or an endpoint response.
    val error = if (nextBoolean()) " ! ${reference(models, features, allowDict = false)}" else ""
    return "rpc $name $shape -> ${reference(models, features, allowUnit = true, allowDict = false)}$error\n"
}

private fun Random.endpoint(name: String, models: List<String>, features: WirespecFeatures): String {
    val request = if (nextBoolean()) " ${simpleReference(models)}" else ""
    // Path parameters keep dromedary names on purpose: the emitter writes them without
    // backticks, so anything needing them cannot survive a round trip.
    val path = List(nextInt(1, 4)) { index ->
        if (nextBoolean()) "/segment$index" else "/{param$index: ${simpleReference(models)}}"
    }.joinToString("")
    val queries = inlineShape(models, features)?.let { " ?{$it}" }.orEmpty()
    val headers = inlineShape(models, features)?.let { " #{$it}" }.orEmpty()
    val responses = STATUSES.shuffled(this).take(nextInt(1, 4)).joinToString("\n") { status ->
        val responseHeaders = inlineShape(models, features)?.let { " #{$it}" }.orEmpty()
        "  $status -> ${reference(models, features, allowUnit = true)}$responseHeaders"
    }
    return "endpoint $name ${pick(METHODS)}$request $path$queries$headers -> {\n$responses\n}\n"
}

private fun Random.shape(models: List<String>, features: WirespecFeatures): String = List(nextInt(1, 5)) { index ->
    annotationBlock(features, indent = "  ") + "  ${fieldName(index)}: ${reference(models, features)}"
}.joinToString(",\n")

private fun Random.inlineShape(models: List<String>, features: WirespecFeatures): String? = when (nextInt(3)) {
    0 -> List(nextInt(1, 3)) { index -> "field$index: ${reference(models, features)}" }.joinToString(", ")
    else -> null
}

private fun Random.fieldName(index: Int): String = when (nextInt(4)) {
    0 -> "field$index"
    1 -> "field_$index"
    2 -> "field-$index"
    else -> "`Field-$index`"
}

private fun Random.enumEntries(): List<String> = List(nextInt(1, 6)) { index ->
    when (nextInt(3)) {
        0 -> "Entry$index"
        1 -> "ENTRY_$index"
        else -> "${index * 7}"
    }
}.distinct()

private fun Random.unionEntries(models: List<String>, self: String): List<String> = models
    .filterNot { it == self }
    .shuffled(this)
    .take(nextInt(2, 4))

private fun Random.refinedReference(): String = when (nextInt(6)) {
    0 -> "String"
    1 -> "String${pick(STRING_CONSTRAINTS)}"
    2 -> "Boolean"
    3 -> "Bytes"
    4 -> "${pick(listOf("Integer", "Integer32"))}${pick(INTEGER_BOUNDS)}"
    else -> "${pick(listOf("Number", "Number32"))}${pick(NUMBER_BOUNDS)}"
}

private fun Random.simpleReference(models: List<String>): String = pick(listOf("String", "Integer", "Boolean") + models)

private fun Random.reference(models: List<String>, features: WirespecFeatures, allowUnit: Boolean = false, allowDict: Boolean = true): String {
    val base = baseReference(models, features, allowUnit)
    return when (nextInt(6)) {
        0 -> if (allowDict) "{ $base }" + (if (nextBoolean()) "?" else "") else base + nullable(base)
        1 -> "$base[]" + if (nextBoolean()) "?" else ""
        else -> base + nullable(base)
    }
}

private fun Random.baseReference(models: List<String>, features: WirespecFeatures, allowUnit: Boolean): String {
    val unit = if (allowUnit) listOf("Unit") else emptyList()
    val base = pick(listOf("String", "Boolean", "Bytes", "Integer", "Integer32", "Number", "Number32", "Any") + models + unit)
    return if (!features.constrainedReferences || !nextBoolean()) {
        base
    } else {
        when (base) {
            "String" -> "String${pick(STRING_CONSTRAINTS)}"
            "Integer", "Integer32" -> "$base${pick(INTEGER_BOUNDS)}"
            "Number", "Number32" -> "$base${pick(NUMBER_BOUNDS)}"
            else -> base
        }
    }
}

/**
 * `String(/re/)?` does not parse: `parsePrimitiveType` evaluates `isNullable` before it looks
 * for the constraint, so the trailing `?` is left on the input. Never follow a constraint
 * directly with a `?`.
 */
private fun Random.nullable(base: String): String = if (base.endsWith(")") || !nextBoolean()) "" else "?"

private fun Random.metadata(features: WirespecFeatures): String = annotationBlock(features, indent = "") + comment(features)

private fun Random.annotationBlock(features: WirespecFeatures, indent: String): String = when {
    !features.annotations -> ""
    else -> List(nextInt(0, 3)) { "$indent${annotation()}\n" }.joinToString("")
}

private fun Random.annotation(): String {
    val name = "@${pick(ANNOTATIONS)}"
    return when (nextInt(8)) {
        0 -> name
        1 -> "$name()"
        2 -> "$name(\"${pick(WORDS)}\")"
        3 -> "$name(${pick(KEYS)}: \"${pick(WORDS)}\")"
        4 -> "$name(${pick(KEYS)}: ${nextInt(0, 100)})"
        5 -> "$name(${pick(KEYS)}: ${nextBoolean()})"
        6 -> "$name([\"${pick(WORDS)}\", \"${pick(WORDS)}\"])"
        else -> "$name(${pick(KEYS)}: [\"${pick(WORDS)}\"], ${pick(KEYS)}: ${nextBoolean()})"
    }
}

private fun Random.comment(features: WirespecFeatures): String = when {
    !features.comments || nextInt(3) != 0 -> ""
    nextBoolean() -> "// ${pick(COMMENTS)}\n"
    else -> "/* ${pick(COMMENTS)} */\n"
}

private const val NOISE_CHARS = "abcXYZ019 \t\n{}()[]<>/\\|!?#:,.-_=*@\"'`~%^&+;é中😀"

private fun Random.noise(): String = when (nextInt(4)) {
    0 -> randomChars()
    1 -> mutate(module(WirespecFeatures.all))
    2 -> module(WirespecFeatures.all).let { it.take(nextInt(0, it.length + 1)) }
    else -> randomTokens()
}

private fun Random.randomChars(): String = List(nextInt(0, 400)) { NOISE_CHARS[nextInt(NOISE_CHARS.length)] }.joinToString("")

private fun Random.randomTokens(): String = List(nextInt(0, 60)) {
    pick(listOf("type", "enum", "endpoint", "channel", "rpc", "{", "}", "(", ")", "->", "=", "|", ":", ",", "?", "!", "#", "[]", "GET", "String", "Integer", "Foo", "bar", "/x", "@Ann", "\"s\"", "/re/", "//c\n", "_", "42", "-1.5"))
}.joinToString(" ")

private fun Random.mutate(source: String): String = when {
    source.isEmpty() -> source
    else -> when (nextInt(3)) {
        0 -> source.removeRange(sliceOf(source))
        1 -> source.replaceRange(sliceOf(source), randomChars())
        else -> sliceOf(source).let { source.replaceRange(it, source.substring(it).repeat(2)) }
    }
}

private fun Random.sliceOf(source: String): IntRange {
    val start = nextInt(source.length)
    return start until nextInt(start, source.length) + 1
}

/**
 * Draws [count] deterministic samples and runs [block] on each.
 *
 * Kotest's `checkAll` is a suspend function and only runs under the kotest engine
 * (`./gradlew kotest`), while this repo's CI runs `jvmTest`/`allTests`. Driving the
 * generators from a plain `kotlin.test` test keeps property coverage in the suite that
 * already runs. The fixed seed makes a failure reproducible, and the failing sample is
 * reported verbatim.
 */
public fun <A> Arb<A>.forEachSample(count: Int = 500, seed: Long = 20250911L, block: (A) -> Unit) {
    samples(RandomSource.seeded(seed)).take(count).forEachIndexed { index, sample ->
        runCatching { block(sample.value) }.onFailure { cause ->
            fail("property failed on sample #$index (seed $seed): $cause\n\n${sample.value}\n", cause)
        }
    }
}

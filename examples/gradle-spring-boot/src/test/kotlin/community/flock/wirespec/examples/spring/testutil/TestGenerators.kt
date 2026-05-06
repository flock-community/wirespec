package community.flock.wirespec.examples.spring.testutil

import community.flock.wirespec.examples.spring.generated.generator.MemberGenerator
import community.flock.wirespec.examples.spring.generated.generator.MemberInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectListGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskListGenerator
import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberInput
import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.generated.model.ProjectList
import community.flock.wirespec.examples.spring.generated.model.Task
import community.flock.wirespec.examples.spring.generated.model.TaskInput
import community.flock.wirespec.examples.spring.generated.model.TaskList
import community.flock.wirespec.generator.randomDouble
import community.flock.wirespec.generator.randomEmail
import community.flock.wirespec.generator.randomFirstName
import community.flock.wirespec.generator.randomFullName
import community.flock.wirespec.generator.randomLastName
import community.flock.wirespec.generator.randomLong
import community.flock.wirespec.generator.randomRegex
import community.flock.wirespec.kotlin.Wirespec
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.reflect.KType

/**
 * Drives the IR-emitted `*Generator.kt` factories with a deterministic
 * `Wirespec.Generator` callback. Dispatches `@Generator("name")` field
 * annotations to the `random*` building blocks from `wirespec-tools-generator`.
 */
object TestGenerators {

    fun seededGenerator(seed: Long = 0L): Wirespec.Generator = SeededGenerator(seed)

    fun memberInput(seed: Long = 0L): MemberInput = MemberInputGenerator.generate(seededGenerator(seed), emptyList())
    fun member(seed: Long = 0L): Member = MemberGenerator.generate(seededGenerator(seed), emptyList())
    fun projectList(seed: Long = 0L): ProjectList = ProjectListGenerator.generate(seededGenerator(seed), emptyList())
    fun projectInput(seed: Long = 0L): ProjectInput = ProjectInputGenerator.generate(seededGenerator(seed), emptyList())
    fun project(seed: Long = 0L): Project = ProjectGenerator.generate(seededGenerator(seed), emptyList())
    fun project(id: String, seed: Long = 0L): Project = ProjectGenerator.generate(seededGenerator(seed), listOf(id))
    fun taskInput(seed: Long = 0L): TaskInput = TaskInputGenerator.generate(seededGenerator(seed), emptyList())
    fun task(seed: Long = 0L): Task = TaskGenerator.generate(seededGenerator(seed), emptyList())
    fun task(id: Long, seed: Long = 0L): Task = TaskGenerator.generate(seededGenerator(seed), listOf(id.toString()))
    fun taskList(seed: Long = 0L): TaskList = TaskListGenerator.generate(seededGenerator(seed), emptyList())
}

/**
 * `Wirespec.Generator` implementation that returns deterministic values for every
 * `GeneratorField` variant, mirroring the strategy used in
 * `src/verify/.../VerifyGeneratorTest.kt`.
 */
private class SeededGenerator(private val baseSeed: Long) : Wirespec.Generator {

    // Stack of seeds to inject into a deeper @Seed string. Each frame is pushed
    // by an enclosing Shape and consumed by the first matching string field.
    private val pendingSeeds = ArrayDeque<PendingSeed>()

    // Stack of active "capture an array-element's @Seed value" frames. Only
    // the top frame's @Seed string is captured; outer frames wait their turn.
    private val captures = ArrayDeque<Capture>()

    private data class PendingSeed(val value: String, val target: String)

    private class Capture(val shapePath: List<String>, val fieldName: String) {
        val seed: AtomicReference<String?> = AtomicReference(null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        type: KType,
        field: Wirespec.GeneratorField<T>,
    ): T {
        captureSeedIfMatches(path, field)?.let { return it as T }

        consumePendingSeedIfMatches(path, field)?.let { return it as T }

        val annotations = field.fieldAnnotations()

        if (annotations.any { it["name"] == "Seed" }) {
            path.dropLast(1).lastOrNull()?.let { candidate ->
                seedAnnotationValueFor(field, candidate)?.let { return it as T }
            }
        }

        if (field is Wirespec.GeneratorFieldShape<*>) {
            seedFieldNameOf(field)?.let { seedFieldName ->
                generateSeededShape(path, field, seedFieldName)?.let { return it as T }
            }
        }

        val random = randomFor(path)

        annotations.namedGeneratorOrNull()?.let { return generateByName(it, random) as T }

        return generateLeaf(field, path, random)
    }

    // Generate (and capture) the @Seed value at its natural path during the
    // first pass of an array-element two-pass. The captured value is always
    // stored as a String (paths are `List<String>`); for an Integer @Seed we
    // also return the parsed Long so the field receives its native type.
    private fun captureSeedIfMatches(path: List<String>, field: Wirespec.GeneratorField<*>): Any? {
        val capture = captures.lastOrNull() ?: return null
        if (capture.seed.get() != null) return null
        val expectedPrefix = capture.shapePath + capture.fieldName
        if (path.size < expectedPrefix.size || path.subList(0, expectedPrefix.size) != expectedPrefix) return null
        val random = randomFor(path)
        return when (field) {
            is Wirespec.GeneratorFieldString -> {
                val value = field.regex?.let { randomRegex(it, random) } ?: randomRegex("\\w{1,50}", random)
                capture.seed.set(value)
                value
            }
            is Wirespec.GeneratorFieldInteger -> {
                val value = randomLong(field.min ?: 0, field.max ?: Long.MAX_VALUE, random)
                capture.seed.set(value.toString())
                value
            }
            else -> null
        }
    }

    private fun consumePendingSeedIfMatches(path: List<String>, field: Wirespec.GeneratorField<*>): Any? {
        val pending = pendingSeeds.lastOrNull() ?: return null
        if (pending.target !in path) return null
        return when (field) {
            is Wirespec.GeneratorFieldString -> {
                pendingSeeds.removeLast()
                pending.value
            }
            is Wirespec.GeneratorFieldInteger -> {
                pendingSeeds.removeLast()
                pending.value.toLong()
            }
            else -> null
        }
    }

    // Direct-`@Seed`-on-primitive case (no Refined wrapper): pull the seed
    // from the parent path segment and coerce to the field's native type.
    private fun seedAnnotationValueFor(field: Wirespec.GeneratorField<*>, candidate: String): Any? = when (field) {
        is Wirespec.GeneratorFieldString -> candidate
        is Wirespec.GeneratorFieldInteger -> candidate.toLongOrNull()
        else -> null
    }

    private fun generateSeededShape(
        path: List<String>,
        field: Wirespec.GeneratorFieldShape<*>,
        seedFieldName: String,
    ): Any? {
        val isArrayContext = path.lastOrNull()?.toIntOrNull() != null
        if (isArrayContext && captures.isEmpty()) {
            val capture = Capture(path, seedFieldName)
            val seed = withFrame(captures, capture) {
                field.generate(path)
                capture.seed.get() ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return field.generate(listOf(seed))
        }

        val candidate = path.dropLast(1).lastOrNull() ?: return null
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName)) {
            field.generate(path)
        }
    }

    private fun seedFieldNameOf(field: Wirespec.GeneratorFieldShape<*>): String? =
        field.annotations.entries
            .firstOrNull { (_, anns) -> anns.any { it["name"] == "Seed" } }
            ?.key

    private fun List<Map<String, Any>>.namedGeneratorOrNull(): String? =
        firstOrNull { it["name"] == "Generator" }
            ?.let { (it["parameters"] as? Map<*, *>)?.get("default") as? String }

    private fun <T> generateLeaf(field: Wirespec.GeneratorField<T>, path: List<String>, random: Random): T {
        @Suppress("UNCHECKED_CAST")
        return when (field) {
            is Wirespec.GeneratorFieldString -> (field.regex?.let { randomRegex(it, random) } ?: randomRegex("\\w{1,50}", random)) as T
            is Wirespec.GeneratorFieldInteger -> randomLong(field.min ?: 0, field.max ?: 0, random) as T
            is Wirespec.GeneratorFieldNumber -> randomDouble(field.min ?: 0.0, field.max ?: 0.0, random) as T
            is Wirespec.GeneratorFieldBoolean -> random.nextBoolean() as T
            is Wirespec.GeneratorFieldBytes -> ByteArray(0) as T
            is Wirespec.GeneratorFieldEnum -> field.values.random(random) as T
            is Wirespec.GeneratorFieldUnion -> field.variants.random(random) as T
            is Wirespec.GeneratorFieldArray<*> -> (0..10).map { i -> field.generate(path + "$i") } as T
            is Wirespec.GeneratorFieldNullable<*> -> field.generate(path) as T
            is Wirespec.GeneratorFieldShape<*> -> field.generate(path) as T
            is Wirespec.GeneratorFieldDict<*> -> mapOf("a" to field.generate(path + "a")) as T
        }
    }

    private fun Wirespec.GeneratorField<*>.fieldAnnotations(): List<Map<String, Any>> = when (this) {
        is Wirespec.GeneratorFieldString -> annotations
        is Wirespec.GeneratorFieldInteger -> annotations
        is Wirespec.GeneratorFieldNumber -> annotations
        is Wirespec.GeneratorFieldBoolean -> annotations
        is Wirespec.GeneratorFieldBytes -> annotations
        is Wirespec.GeneratorFieldEnum -> annotations
        is Wirespec.GeneratorFieldUnion -> annotations
        else -> emptyList()
    }

    private fun randomFor(path: List<String>): Random =
        Random(baseSeed xor path.joinToString("/").hashCode().toLong())

    private fun generateByName(name: String, random: Random): String = when (name.lowercase()) {
        "email" -> randomEmail(random)
        "firstname" -> randomFirstName(random)
        "lastname" -> randomLastName(random)
        "fullname" -> randomFullName(random)
        else -> error("Unknown @Generator name: '$name'")
    }
}

private inline fun <F, R> withFrame(stack: ArrayDeque<F>, frame: F, block: () -> R): R {
    val mark = stack.size
    stack.addLast(frame)
    return try {
        block()
    } finally {
        while (stack.size > mark) stack.removeLast()
    }
}

package community.flock.wirespec.examples.spring.testutil

import community.flock.wirespec.examples.spring.generated.generator.MemberGenerator
import community.flock.wirespec.examples.spring.generated.generator.MemberInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskInputGenerator
import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberInput
import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.generated.model.Task
import community.flock.wirespec.examples.spring.generated.model.TaskInput
import community.flock.wirespec.kotlin.Wirespec
import kotlin.random.Random
import kotlin.reflect.KType

/**
 * Drives the IR-emitted `*Generator.kt` factories with a deterministic
 * `Wirespec.Generator` callback. Each generator delegates field-by-field to
 * `generate(path, type, GeneratorField<T>)`, so we just answer that callback
 * with seeded random values.
 *
 * Use `seeded(seed)` for reproducible test data.
 */
object TestGenerators {

    fun seeded(seed: Long = 0L): Wirespec.Generator = SeededGenerator(Random(seed))

    fun memberInput(seed: Long): MemberInput = MemberInputGenerator.generate(listOf("MemberInput"), seeded(seed))
    fun member(seed: Long): Member = MemberGenerator.generate(listOf("Member"), seeded(seed))
    fun projectInput(seed: Long): ProjectInput = ProjectInputGenerator.generate(listOf("ProjectInput"), seeded(seed))
    fun project(seed: Long): Project = ProjectGenerator.generate(listOf("Project"), seeded(seed))
    fun taskInput(seed: Long): TaskInput = TaskInputGenerator.generate(listOf("TaskInput"), seeded(seed))
    fun task(seed: Long): Task = TaskGenerator.generate(listOf("Task"), seeded(seed))
}

/**
 * `Wirespec.Generator` implementation that returns deterministic values for every
 * `GeneratorField` variant, mirroring the strategy used in
 * `src/verify/.../VerifyGeneratorTest.kt`.
 */
private class SeededGenerator(private val random: Random) : Wirespec.Generator {

    private var counter: Int = 0

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> generate(
        path: List<String>,
        type: KType,
        field: Wirespec.GeneratorField<T>,
        annotations: List<Map<String, Any?>>,
    ): T {
        if (annotations.any { it["name"] == "Email" }) {
            counter += 1
            return "user-$counter@example.com" as T
        }
        return when (field) {
            is Wirespec.GeneratorFieldString -> generateString(field) as T
            is Wirespec.GeneratorFieldInteger -> randomLong(field.min, field.max) as T
            is Wirespec.GeneratorFieldNumber -> randomDouble(field.min, field.max) as T
            Wirespec.GeneratorFieldBoolean -> random.nextBoolean() as T
            Wirespec.GeneratorFieldBytes -> ByteArray(0) as T
            is Wirespec.GeneratorFieldEnum -> field.values.random(random) as T
            is Wirespec.GeneratorFieldUnion -> field.variants.random(random) as T
            is Wirespec.GeneratorFieldArray -> 1 as T // produce 1-element collections
            is Wirespec.GeneratorFieldNullable -> false as T // false => "not null", emit a value
            is Wirespec.GeneratorFieldDict -> 1 as T
        }
    }

    private fun generateString(field: Wirespec.GeneratorFieldString): String {
        val regex = field.regex
        return if (regex != null && regex.contains("[0-9a-fA-F]{8}")) {
            // Refined UUID-shaped fields: build a deterministic UUID-like string from the seed.
            val r = random
            val a = (0 until 8).joinToString("") { hex(r) }
            val b = (0 until 4).joinToString("") { hex(r) }
            val c = (0 until 4).joinToString("") { hex(r) }
            val d = (0 until 4).joinToString("") { hex(r) }
            val e = (0 until 12).joinToString("") { hex(r) }
            "$a-$b-$c-$d-$e"
        } else {
            counter += 1
            "value-$counter-${random.nextInt(1_000_000)}"
        }
    }

    private fun hex(r: Random): String = "0123456789abcdef"[r.nextInt(16)].toString()

    private fun randomLong(min: Long?, max: Long?): Long {
        val low = min ?: 0L
        val high = (max ?: (low + 1_000)).coerceAtLeast(low + 1)
        return random.nextLong(low, high)
    }

    private fun randomDouble(min: Double?, max: Double?): Double {
        val low = min ?: 0.0
        val high = (max ?: (low + 1_000.0)).coerceAtLeast(low + 0.000001)
        return random.nextDouble(low, high)
    }
}

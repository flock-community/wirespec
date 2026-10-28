package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Constraint.RegExp
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringFormatTest {

    private fun matching(format: String, vararg values: String) = StringFormat.of(format)!!.regExp.value
        .removeSurrounding("/")
        .let(::Regex)
        .let { regex -> values.filter(regex::matches) }

    @Test
    fun lookup() {
        StringFormat.entries.forEach { format ->
            StringFormat.of(format.label) shouldBe format
            StringFormat.of(format.regExp) shouldBe format
        }
        StringFormat.of("time") shouldBe null
        StringFormat.of(RegExp("/^[0-9]+$/")) shouldBe null
    }

    @Test
    fun date() {
        matching("date", "2024-02-29", "1999-12-31", "0001-01-01").shouldContainExactly("2024-02-29", "1999-12-31", "0001-01-01")
        matching("date", "2024-13-01", "2024-00-10", "2024-01-32", "24-01-01", "2024-1-01", "2024-01-01T00:00:00Z").shouldContainExactly()
    }

    @Test
    fun dateTime() {
        matching(
            "date-time",
            "2024-02-29T13:45:30Z",
            "2024-02-29T13:45:30.123Z",
            "2024-02-29T00:00:00+01:00",
            "2024-02-29T23:59:59.123456-05:30",
        ).shouldContainExactly(
            "2024-02-29T13:45:30Z",
            "2024-02-29T13:45:30.123Z",
            "2024-02-29T00:00:00+01:00",
            "2024-02-29T23:59:59.123456-05:30",
        )
        matching(
            "date-time",
            "2024-02-29",
            "2024-02-29T13:45Z",
            "2024-02-29T24:00:00Z",
            "2024-02-29 13:45:30Z",
            "2024-02-29T13:45:30",
            "2024-02-29T13:45:30+0100",
            "2024-02-29T13:45:30.Z",
        ).shouldContainExactly()
    }

    @Test
    fun uuid() {
        matching(
            "uuid",
            "123e4567-e89b-12d3-a456-426614174000",
            "00000000-0000-0000-0000-000000000000",
            "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF",
        ).shouldContainExactly(
            "123e4567-e89b-12d3-a456-426614174000",
            "00000000-0000-0000-0000-000000000000",
            "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF",
        )
        matching(
            "uuid",
            "123e4567e89b12d3a456426614174000",
            "123e4567-e89b-12d3-a456-42661417400g",
            "{123e4567-e89b-12d3-a456-426614174000}",
        ).shouldContainExactly()
    }

    @Test
    fun uuidVersions() {
        val samples = listOf(
            "C232AB00-9414-11EC-B3C8-9F6BDECED846",
            "000003e8-cbb9-21ea-b201-00045a86c8a1",
            "5df41881-3aed-3515-88a7-2f4a814cf09e",
            "919108f7-52d1-4320-9bac-f847db4148a8",
            "2ed6657d-e927-568b-95e1-2665a8aea6a2",
            "1EC9414C-232A-6B00-B3C8-9F6BDECED846",
            "017F22E2-79B0-7CC3-98C4-DC0C0C07398F",
            "5c146b14-3c52-8afd-938a-375d0df1fbf6",
        )
        samples.forEachIndexed { index, sample ->
            matching("uuid-v${index + 1}", *samples.toTypedArray()).shouldContainExactly(sample)
        }
        matching("uuid", *samples.toTypedArray()).shouldContainExactly(samples)
        matching("uuid-v4", "919108f7-52d1-4320-cbac-f847db4148a8", "919108f7-52d1-4320-7bac-f847db4148a8").shouldContainExactly()
    }
}

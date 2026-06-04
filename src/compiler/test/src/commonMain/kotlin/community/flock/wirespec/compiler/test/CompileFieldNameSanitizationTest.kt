package community.flock.wirespec.compiler.test

/**
 * Exercises every field-name shape that the tokenizer accepts but that needs
 * language-specific handling to keep its JSON wire name intact.
 *
 * The field identifier grammar (see LanguageSpec) is `^[a-z`][a-zA-Z0-9_\-`]*`, so a
 * backtick-quoted field name may contain letters, digits, `_` and `-` — but not dots
 * or spaces. The interesting (and currently broken) cases all involve `-`, because
 * `IrConverter.toName()` splits on `[.\s-]+` and the emitters then re-case the parts,
 * dropping the original wire name (`house-number` -> `houseNumber`).
 *
 * Note: we deliberately avoid a field pair that differs only by `-` vs `_` (e.g.
 * `house-number` together with `house_number`). Identifier-restricted languages such as
 * Rust map both to the same `house_number` field, which is an unrepresentable collision.
 */
object CompileFieldNameSanitizationTest : Fixture {

    override val source =
        // language=ws
        """
        |type FieldNames {
        |  street: String,
        |  postalCode: String,
        |  snake_cased: String,
        |  `house-number`: String,
        |  `x-api-key`: String,
        |  `HTTP-version`: String,
        |  `1st-line`: String,
        |  `BODY_TYPE`: String,
        |  `type`: String
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}

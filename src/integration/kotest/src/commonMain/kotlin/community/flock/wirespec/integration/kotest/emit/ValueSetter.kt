package community.flock.wirespec.integration.kotest.emit

import community.flock.wirespec.ir.core.StructBuilder
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.ir.core.Type as IrType

/**
 * Every override slot in the generated DSL is a `Gen<T>?` property, so pinning a constant
 * otherwise reads `name = Arb.constant("Autumn promo")`. Next to each slot we emit a
 * same-named single-arg function so the common case is just `name("Autumn promo")`:
 *
 * ```
 * public var name: Gen<String>? = null
 * public fun name(value: String) {
 *   this.name = Arb.constant(value)
 * }
 * ```
 *
 * A property and a function may share a name in Kotlin, so both forms stay available: the
 * function for a fixed value, the property for a real `Gen`/`Arb`.
 *
 * Emitting these requires `io.kotest.property.Arb` and `io.kotest.property.arbitrary.constant`
 * to be imported by the surrounding file.
 */
internal fun StructBuilder.valueSetter(fieldName: String, type: IrType) {
    val escaped = fieldName.escapeKotlinIdentifier()
    function(fieldName) {
        visibility(Visibility.PUBLIC)
        arg("value", type)
        raw("this.$escaped = Arb.constant(value)")
    }
}

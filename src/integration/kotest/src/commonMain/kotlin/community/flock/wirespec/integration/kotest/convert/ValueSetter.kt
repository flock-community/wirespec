package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.ir.core.StructBuilder
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.ir.core.Type as IrType

/** Emits a same-named single-arg `<field>(value)` function next to each `Gen<T>?` slot for pinning a constant. */
internal fun StructBuilder.valueSetter(fieldName: String, type: IrType) {
    val escaped = fieldName.escapeKotlinIdentifier()
    function(fieldName) {
        visibility(Visibility.PUBLIC)
        arg("value", type)
        raw("this.$escaped = Arb.constant(value)")
    }
}

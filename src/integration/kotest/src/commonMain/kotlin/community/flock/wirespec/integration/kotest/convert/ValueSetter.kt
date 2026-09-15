package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.ir.StructBuilder
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.compiler.core.ir.Type as IrType

internal fun StructBuilder.valueSetter(fieldName: String, type: IrType) {
    val escaped = fieldName.escapeKotlinIdentifier()
    function(fieldName) {
        visibility(Visibility.PUBLIC)
        arg("value", type)
        raw("this.$escaped = Arb.constant(value)")
    }
}

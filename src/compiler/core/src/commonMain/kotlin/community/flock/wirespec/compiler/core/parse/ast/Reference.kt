package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Precision.P64
import kotlin.jvm.JvmInline

sealed interface Reference : Value<String> {
    val isNullable: Boolean

    fun copy(isNullable: Boolean? = null) = when (this) {
        is Any -> copy(isNullable = isNullable ?: this.isNullable)
        is Custom -> copy(isNullable = isNullable ?: this.isNullable)
        is Dict -> copy(isNullable = isNullable ?: this.isNullable)
        is Iterable -> copy(isNullable = isNullable ?: this.isNullable)
        is Primitive -> copy(isNullable = isNullable ?: this.isNullable)
        is Unit -> copy(isNullable = isNullable ?: this.isNullable)
    }

    data class Any(
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Any"
    }

    data class Unit(
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Unit"
    }

    data class Dict(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Dict"
    }

    data class Iterable(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Iterable"
    }

    data class Custom(
        override val value: String,
        override val isNullable: Boolean,
    ) : Reference

    data class Primitive(
        val type: Type,
        override val isNullable: Boolean,
    ) : Reference {

        sealed interface Type {
            val name: kotlin.String

            enum class Precision { P32, P64 }

            sealed interface Constraint {
                @JvmInline
                value class RegExp(override val value: kotlin.String) :
                    Value<kotlin.String>,
                    Constraint

                data class Bound(val min: kotlin.String?, val max: kotlin.String?) : Constraint
            }

            data class String(val constraint: Constraint.RegExp?) : Type {
                override val name = "String"
            }

            data class Integer(val precision: Precision = P64, val constraint: Constraint.Bound?) : Type {
                override val name = "Integer"
            }

            data class Number(val precision: Precision = P64, val constraint: Constraint.Bound?) : Type {
                override val name = "Number"
            }

            data object Boolean : Type {
                override val name = "Boolean"
            }

            data object Bytes : Type {
                override val name = "Bytes"
            }
        }

        override val value = type.name
    }
}

package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Precision.P64
import kotlin.jvm.JvmInline

public sealed interface Reference : Value<String> {
    public val isNullable: Boolean

    public fun copy(isNullable: Boolean? = null): Reference = when (this) {
        is Any -> copy(isNullable = isNullable ?: this.isNullable)
        is Custom -> copy(isNullable = isNullable ?: this.isNullable)
        is Dict -> copy(isNullable = isNullable ?: this.isNullable)
        is Iterable -> copy(isNullable = isNullable ?: this.isNullable)
        is Primitive -> copy(isNullable = isNullable ?: this.isNullable)
        is Unit -> copy(isNullable = isNullable ?: this.isNullable)
    }

    public data class Any(
        override val isNullable: Boolean,
    ) : Reference {
        override val value: String = "Any"
    }

    public data class Unit(
        override val isNullable: Boolean,
    ) : Reference {
        override val value: String = "Unit"
    }

    public data class Dict(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value: String = "Dict"
    }

    public data class Iterable(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value: String = "Iterable"
    }

    public data class Custom(
        override val value: String,
        override val isNullable: Boolean,
    ) : Reference

    public data class Primitive(
        val type: Type,
        override val isNullable: Boolean,
    ) : Reference {

        public sealed interface Type {
            public val name: kotlin.String

            public enum class Precision { P32, P64 }

            public sealed interface Constraint {
                @JvmInline
                public value class RegExp(override val value: kotlin.String) :
                    Value<kotlin.String>,
                    Constraint

                public data class Bound(val min: kotlin.String?, val max: kotlin.String?) : Constraint
            }

            public interface HasConstraint<C : Constraint> {
                public val constraint: C?
            }

            public data class String(val constraint: Constraint.RegExp?) : Type {
                override val name: kotlin.String = "String"
            }

            public data class Integer(val precision: Precision = P64, override val constraint: Constraint.Bound?) :
                HasConstraint<Constraint.Bound>,
                Type {
                override val name: kotlin.String = "Integer"
            }

            public data class Number(val precision: Precision = P64, override val constraint: Constraint.Bound?) :
                HasConstraint<Constraint.Bound>,
                Type {
                override val name: kotlin.String = "Number"
            }

            public data object Boolean : Type {
                override val name: kotlin.String = "Boolean"
            }

            public data object Bytes : Type {
                override val name: kotlin.String = "Bytes"
            }
        }

        override val value: kotlin.String = type.name
    }
}

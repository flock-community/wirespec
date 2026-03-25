package community.flock.wirespec.ir.core

fun Expression.fieldCall(field: String): FieldCall = FieldCall(receiver = this, field = Name.of(field))

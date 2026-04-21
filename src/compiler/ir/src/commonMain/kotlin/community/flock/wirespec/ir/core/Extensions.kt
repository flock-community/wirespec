package community.flock.wirespec.ir.core

fun Expression.fieldCall(field: String): FieldCall = FieldCall(receiver = this, field = Name.of(field))
fun <T:HasElements> T.plus(element: Element): T = when (this) {
    is File -> copy(elements = elements + element) as T
    is Struct -> copy(elements = elements + element) as T
    is Namespace -> copy(elements = elements + element) as T
    is Interface -> copy(elements = elements + element) as T
    is Enum -> copy(elements = elements + element) as T
}
package community.flock.wirespec.compare

sealed class Paired<A> {
    class Left<A>(val key: String, val left: A) : Paired<A>()
    class Right<A>(val key: String, val right: A) : Paired<A>()
    class Both<A>(val key: String, val left: A, val right: A) : Paired<A>()
}

internal fun <A> Pair<List<A>, List<A>>.pairBy(f: (a: A) -> String): List<Paired<A>> {
    val leftMap = first.groupBy { f(it) }
    val rightMap = second.groupBy { f(it) }
    val allKeys = leftMap.keys + rightMap.keys
    return allKeys.map {
        when {
            leftMap[it] == null && rightMap[it] != null -> Paired.Right(it, rightMap[it]!!.first())
            leftMap[it] != null && rightMap[it] == null -> Paired.Left(it, leftMap[it]!!.first())
            else -> Paired.Both(it, leftMap[it]!!.first(), rightMap[it]!!.first())
        }
    }.toList()
}

internal fun <A, B> Paired<A>.into(f: (a: A) -> B): Paired<B> =
    when(this){
        is Paired.Left -> Paired.Left(key, f(left))
        is Paired.Right -> Paired.Right(key, f(right))
        is Paired.Both -> Paired.Both(key, f(left), f(right))
    }

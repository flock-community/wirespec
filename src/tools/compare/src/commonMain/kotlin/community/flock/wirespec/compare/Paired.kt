package community.flock.wirespec.compare

sealed class Paired<A> {
    class Old<A>(val key: String, val old: A) : Paired<A>()
    class New<A>(val key: String, val new: A) : Paired<A>()
    class Both<A>(val key: String, val old: A, val new: A) : Paired<A>()
}

internal fun <A> Pair<List<A>, List<A>>.pairBy(f: (a: A) -> String): List<Paired<A>> {
    val oldMap = first.groupBy { f(it) }
    val newMap = second.groupBy { f(it) }
    val allKeys = oldMap.keys + newMap.keys
    return allKeys.map {
        when {
            oldMap[it] == null && newMap[it] != null -> Paired.New(it, newMap[it]!!.first())
            oldMap[it] != null && newMap[it] == null -> Paired.Old(it, oldMap[it]!!.first())
            else -> Paired.Both(it, oldMap[it]!!.first(), newMap[it]!!.first())
        }
    }.toList()
}

internal fun <A, B> Paired<A>.into(f: (a: A) -> B): Paired<B> =
    when (this) {
        is Paired.Old -> Paired.Old(key, f(old))
        is Paired.New -> Paired.New(key, f(new))
        is Paired.Both -> Paired.Both(key, f(old), f(new))
    }

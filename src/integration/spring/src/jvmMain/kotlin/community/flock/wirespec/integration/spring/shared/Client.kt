package community.flock.wirespec.integration.spring.shared

fun Map<String, List<String>>.filterNotEmpty(): Map<String, List<String>> = filter { it.value.isNotEmpty() }
package community.flock.wirespec.integration.jackson.common

import community.flock.wirespec.compiler.core.emit.common.Keywords

fun translater(reserved: Keywords): String.() -> String = {
    val keywords = reserved.reservedKeywords.map { "_$it" }
    if (this in keywords) drop(1) else this
}

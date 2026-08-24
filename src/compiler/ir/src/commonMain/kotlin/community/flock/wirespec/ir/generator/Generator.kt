package community.flock.wirespec.ir.generator

import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.ir.core.Element

/**
 * Renders an IR tree to target-language source. Each language's generator is also the single
 * source of truth for that language's reserved keywords ([Keywords]); emitters and extensions
 * delegate to it instead of keeping their own copies.
 */
interface Generator : Keywords {
    fun generate(element: Element): String
}

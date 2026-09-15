package community.flock.wirespec.compiler.core.ir.generator

import community.flock.wirespec.compiler.core.ir.Element

/**
 * Renders an IR tree to target-language source. Each language's generator also implements
 * [community.flock.wirespec.compiler.core.emit.Keywords] as the single source of truth for that
 * language's reserved keywords; emitters and extensions delegate to it instead of keeping their
 * own copies.
 */
public interface Generator {
    public fun generate(element: Element): String
}

package community.flock.wirespec.ir.generator

import community.flock.wirespec.ir.core.Element

interface Generator {
    fun generate(element: Element): String
}

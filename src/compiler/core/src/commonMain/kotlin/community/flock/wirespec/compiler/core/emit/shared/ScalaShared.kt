package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING

data object ScalaShared : Shared {
    override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.scala"

    override val source = """
        |package $packageString
        |
        |object Wirespec
        |
    """.trimMargin()
}

package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING

data object ScalaShared : Shared {
    override val source = """
        |package $DEFAULT_SHARED_PACKAGE_STRING.scala
        |
        |object Wirespec
        |
    """.trimMargin()
}

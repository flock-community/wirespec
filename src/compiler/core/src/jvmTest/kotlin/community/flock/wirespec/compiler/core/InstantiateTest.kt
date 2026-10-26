package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class InstantiateTest {

    class Defaults(val packageName: PackageName = PackageName("default"), val emitShared: EmitShared = EmitShared(false))

    class Extension(val packageName: PackageName, val language: FileExtension)

    class NoArguments

    @Test
    fun fillsParametersByType() {
        Extension::class.java.instantiate<Extension>(EmitShared(true), FileExtension.Java, PackageName("given"))
            .run {
                packageName.value shouldBe "given"
                language shouldBe FileExtension.Java
            }
    }

    @Test
    fun prefersGivenValuesOverDefaults() {
        Defaults::class.java.instantiate<Defaults>(PackageName("given"), EmitShared(true))
            .run {
                packageName.value shouldBe "given"
                emitShared.value shouldBe true
            }
    }

    @Test
    fun ignoresArgumentsNoConstructorTakes() {
        NoArguments::class.java.instantiate<NoArguments>(PackageName("unused"), EmitShared(true))::class shouldBe NoArguments::class
    }

    @Test
    fun failsWhenAParameterCannotBeFilled() {
        shouldThrow<IllegalArgumentException> { Extension::class.java.instantiate<Extension>(PackageName("given")) }
            .message shouldContain "no public constructor"
    }

    @Test
    fun failsWhenTheInstanceHasAnotherType() {
        shouldThrow<IllegalArgumentException> { NoArguments::class.java.instantiate<Extension>() }
            .message shouldContain "is not a"
    }
}

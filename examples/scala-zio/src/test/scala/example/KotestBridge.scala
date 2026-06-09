package example

import community.flock.wirespec.integration.kotest.{
  KotestWirespecGeneratorBuilder,
  KotestWirespecScalaGeneratorKt
}
import community.flock.wirespec.scala.Wirespec
import kotlin.Unit
import kotlin.jvm.functions.Function1

/** Scala-side facade for `kotestWirespecScalaGenerator(...)`. Hides the
  * Kotlin `Function1<Builder, Unit>` block parameter and the `Any` cast
  * so spec source stays readable. */
object KotestBridge {

  /** Generator with no custom `@Generator(...)` registrations. */
  def generator(seed: Long = 0L): Wirespec.Generator =
    KotestWirespecScalaGeneratorKt
      .kotestWirespecScalaGenerator(seed, NoOp)
      .asInstanceOf[Wirespec.Generator]

  private val NoOp: Function1[KotestWirespecGeneratorBuilder, Unit] =
    new Function1[KotestWirespecGeneratorBuilder, Unit] {
      override def invoke(b: KotestWirespecGeneratorBuilder): Unit =
        Unit.INSTANCE
    }
}

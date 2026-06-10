package community.flock.wirespec.integration.kotest

import community.flock.wirespec.scala.Wirespec

/** Scala-facing factory for Scala-emitted code: returns a
  * `community.flock.wirespec.scala.Wirespec.Generator`, which is what
  * Scala IR-emitted `*Generator.generate(gen, …)` factories expect.
  *
  * {{{
  * val gen = KotestWirespec.generator(seed = 1L)
  * val member = MemberGenerator.generate(gen, List.empty)
  * }}}
  *
  * Overrides are registered through `configure`:
  *
  * {{{
  * val gen = KotestWirespec.generator(
  *   seed = 1L,
  *   configure = b => b.registerPath(Array("users", "*", "id"), "FIXED-ID"),
  * )
  * }}}
  *
  * Wraps [[KotestWirespecScalaGeneratorKt.kotestWirespecScalaGenerator]],
  * hiding its Kotlin `Function1` block parameter and `Any` return type.
  */
object KotestWirespec {

  def generator(
      seed: Long = 0L,
      configure: KotestWirespecGeneratorBuilder => Unit = _ => (),
  ): Wirespec.Generator =
    KotestWirespecScalaGeneratorKt
      .kotestWirespecScalaGenerator(
        seed,
        (b: KotestWirespecGeneratorBuilder) => { configure(b); kotlin.Unit.INSTANCE },
      )
      .asInstanceOf[Wirespec.Generator]
}

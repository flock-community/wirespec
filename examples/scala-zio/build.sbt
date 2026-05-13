val scala3Version = "3.3.4"
val zioVersion = "2.1.14"
val zioHttpVersion = "3.0.1"
val circeVersion = "0.14.10"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-zio",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    // Wirespec code generation
    Compile / sourceGenerators += wirespecGenerate.taskValue,
    Compile / managedSourceDirectories += baseDirectory.value / "target" / "generated-sources",
  )

lazy val wirespecGenerate = taskKey[Seq[File]]("Generate Scala sources from Wirespec/OpenAPI")

wirespecGenerate := {
  val log = streams.value.log
  val outDir = baseDirectory.value / "target" / "generated-sources"

  val genScript = baseDirectory.value / "gen.sh"
  if (!genScript.exists()) {
    sys.error(s"gen.sh not found at ${genScript.absolutePath}")
  }

  log.info("Running Wirespec code generation...")
  import scala.sys.process._
  val exitCode = Process(Seq("bash", genScript.absolutePath), baseDirectory.value).!
  if (exitCode != 0) {
    sys.error(s"gen.sh failed with exit code $exitCode")
  }

  // Collect all generated .scala files
  (outDir ** "*.scala").get
}

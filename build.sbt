import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings


val scalaCOptions = Seq(
  //"-Werror", TODO (APB-9550) find a replacement for the deprecated 'name' retrieval
  "-Wdead-code",
  "-feature",
  "-language:implicitConversions",
  "-Xlint",
  "-Xlint:-byname-implicit",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=*html:w", // silence html warnings as they are wrong
  "-Wconf:cat=unused-privates:s",
  "-Wconf:msg=match may not be exhaustive:is", // summarize warnings about non-exhaustive pattern matching
)

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.16"

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentservicesaccount.views.html.components._",
  "uk.gov.hmrc.agentservicesaccount.utils.ViewUtils._",
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin, SbtTwirl)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9401,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.agentservicesaccount.models.AmlsStatus"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    scalacOptions ++= scalaCOptions,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    CodeCoverageSettings.settings
  )
  .settings(
    Test / parallelExecution := false,
    Test / logBuffered := false,
  )


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(Test / logBuffered := false)

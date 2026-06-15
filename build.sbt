import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings


val scalaCOptions = Seq(
  "-Wconf:msg=Flag.*repeatedly:s", // silence warnings about compiler options being invoked repeatedly
  "-feature",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=routes/.*:s", // silence warnings from routes files
  "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s",
  "-Wconf:msg=Alphanumeric method .* is not declared infix:s",
)

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "3.7.4"

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentservicesaccount.views.html.components._",
  "uk.gov.hmrc.agentservicesaccount.utils.ViewUtils._",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin, SbtTwirl)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9401,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.agentservicesaccount.models.AmlsStatus",
      "uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime"
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
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
  )


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(Test / logBuffered := false)

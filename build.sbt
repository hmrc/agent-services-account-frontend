import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.SbtAutoBuildPlugin

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentservicesaccount.views.html.components._",
  "uk.gov.hmrc.agentservicesaccount.utils.ViewUtils._",
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    majorVersion := 1,
    scalacOptions ++= Seq(
      "-Werror",
      "-Wdead-code",
      "-feature",
      "-language:implicitConversions",
      "-Xlint",
      "-Xlint:-byname-implicit",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
      "-Wconf:cat=unused-privates:s",
      "-Wconf:msg=match may not be exhaustive:is", // summarize warnings about non-exhaustive pattern matching
    ),
    PlayKeys.playDefaultPort := 9401,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    CodeCoverageSettings.settings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.agentservicesaccount.models.AmlsStatus"
    ),
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

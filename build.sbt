import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val wartRemoverSettings = {
  val wartRemoverWarning = {
    val warningWarts = Seq(
      Wart.JavaSerializable,
      //Wart.StringPlusAny,
      Wart.AsInstanceOf,
      Wart.IsInstanceOf
      //Wart.Any
    )
    wartremoverWarnings in (Compile, compile) ++= warningWarts
  }

  val wartRemoverError = {
    // Error
    val errorWarts = Seq(
      Wart.ArrayEquals,
      Wart.AnyVal,
      Wart.EitherProjectionPartial,
      Wart.Enumeration,
      Wart.ExplicitImplicitTypes,
      Wart.FinalVal,
      Wart.JavaConversions,
      Wart.JavaSerializable,
      //Wart.LeakingSealed,
      Wart.MutableDataStructures,
      Wart.Null,
      //Wart.OptionPartial,
      Wart.Recursion,
      Wart.Return,
      //Wart.TraversableOps,
      Wart.TryPartial,
      Wart.Var,
      Wart.While)

    wartremoverErrors in (Compile, compile) ++= errorWarts
  }

  Seq(
    wartRemoverError,
    wartRemoverWarning,
    wartremoverErrors in (Test, compile) --= Seq(Wart.Any, Wart.Equals, Wart.Null, Wart.NonUnitStatements, Wart.PublicInference),
    wartremoverExcluded ++=
      routes.in(Compile).value ++
        (baseDirectory.value / "it").get ++
        (baseDirectory.value / "test").get ++
        Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala")
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.12.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.35.0-play-25",
  "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-25",
  "uk.gov.hmrc" %% "auth-client" % "2.31.0-play-25",
  "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "3.8.0",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.15.0-play-25",
  "uk.gov.hmrc" %% "play-language" % "4.2.0-play-25"
)

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-25" % scope,
  "org.scalatest" %% "scalatest" % "3.0.7" % scope,
  "org.mockito" % "mockito-core" % "2.27.0" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.23.2" % scope,
  "org.jsoup" % "jsoup" % "1.12.1" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.11.11",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions"),
    PlayKeys.playDefaultPort := 9401,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false
  )
  .settings(wartRemoverSettings: _*)
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
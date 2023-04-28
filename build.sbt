import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
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
    Compile / compile / wartremoverWarnings ++= warningWarts
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
      Wart.Null,
      Wart.Recursion,
      Wart.Return,
      Wart.TryPartial,
      Wart.Var,
      Wart.While)

    Compile / compile / wartremoverErrors ++= errorWarts
  }

  Seq(
    wartRemoverError,
    wartRemoverWarning,
    Test / compile / wartremoverErrors --= Seq(Wart.Any, Wart.Equals, Wart.Null, Wart.NonUnitStatements, Wart.PublicInference),
    wartremoverExcluded ++=
      (Compile / routes).value ++
        (baseDirectory.value / "it").get ++
        (baseDirectory.value / "test").get ++
        Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala")
  )
}

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentservicesaccount.views.html.components._",
  "uk.gov.hmrc.agentservicesaccount.utils.ViewUtils._",
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    scalacOptions ++= Seq(
      "-Werror",
      "-Wdead-code",
      "-Xlint",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
      "-Wconf:cat=deprecation:s",
      "-Wconf:cat=unused-privates:s",
      "-Wconf:msg=match may not be exhaustive:is", // summarize warnings about non-exhaustive pattern matching
    ),
    PlayKeys.playDefaultPort := 9401,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .settings(wartRemoverSettings: _*)
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

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
      //Wart.LeakingSealed,
//      Wart.MutableDataStructures,
      Wart.Null,
      //Wart.OptionPartial,
      Wart.Recursion,
      Wart.Return,
      //Wart.TraversableOps,
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
)

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.3.0",
  "uk.gov.hmrc" %% "play-partials"              % "8.3.0-play-28",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring"   % "4.8.0-play-28",
  "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "0.47.0-play-28",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "3.24.0-play-28"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % "test",
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.1" % scope,
  "org.jsoup" %  "jsoup" % "1.13.1" % Test,
  "org.jsoup" % "jsoup" % "1.12.1" % scope,
  "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-services-account-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes;uk/gov/hmrc/agentservicesaccount/FrontendModule"),
    PlayKeys.playDefaultPort := 9401,
    resolvers := Seq(
      Resolver.typesafeRepo("releases"),
    ),
    resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2",
    resolvers += "HMRC-local-artefacts-maven" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases-local",
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
    resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.8" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.8" % Provided cross CrossVersion.full
    ),
    publishingSettings,
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

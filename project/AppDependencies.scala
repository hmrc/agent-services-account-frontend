import sbt._

object AppDependencies {
  private val mongoVer: String = "1.7.0"
  private val bootstrapVer: String = "7.23.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVer,
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "agent-kenshoo-monitoring"   % "5.5.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "1.15.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % mongoVer,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-28" % "8.5.0",
    "org.julienrf"      %% "play-json-derived-codecs"   % "7.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"         % "5.1.0"      % "test",
    "org.scalatestplus"      %% "mockito-3-12"               % "3.2.10.0"   % "test",
    "org.scalatest"          %% "scalatest"                  % "3.2.15"     % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest"    % "1.17.12"    % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"    % mongoVer     % "test, it",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"     % bootstrapVer % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"              % "2.26.2"     % "test, it",
    "org.jsoup"              %  "jsoup"                      % "1.15.4"     % "test, it",
    "org.scalamock"          %% "scalamock"                  % "5.2.0"      % "test, it",
    "com.vladsch.flexmark"   %  "flexmark-all"               % "0.64.8"     % "test, it"
  )

}

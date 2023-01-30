import sbt._

object AppDependencies {
  private val mongoVer: String = "0.74.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "7.13.0",
    "uk.gov.hmrc"       %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "agent-kenshoo-monitoring"   % "4.8.0-play-28",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "0.57.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % mongoVer,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "5.5.0-play-28"
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"         % "5.1.0"    % "test",
    "org.scalatestplus"      %% "mockito-3-12"               % "3.2.10.0" % "test",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"    % mongoVer   % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"              % "2.26.1"   % "test, it",
    "org.jsoup"              %  "jsoup"                      % "1.15.3"   % "test, it",
    "com.vladsch.flexmark"   %  "flexmark-all"               % "0.35.10"  % "test, it"
  )

}

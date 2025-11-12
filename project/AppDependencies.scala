import sbt.*

object AppDependencies {
  private val mongoVersion: String = "2.10.0"
  private val bootstrapVersion: String = "10.4.0"
  private val enumeratumVersion = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-partials-play-30"      % "10.2.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.20.0",
    "com.beachape"      %% "enumeratum-play"            % enumeratumVersion,
    "org.julienrf"      %% "play-json-derived-codecs"   % "11.0.0",
    "org.apache.commons" % "commons-text"               % "1.14.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"        % "8.4.0",
    "uk.gov.hmrc"       %% "domain-play-30"             % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"    % mongoVersion      % Test,
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"     % bootstrapVersion  % Test,
    "org.mockito"       %% "mockito-scala-scalatest"    % "2.0.0"           % Test,
    "org.scalamock"     %% "scalamock"                  % "7.5.0"           % Test
  )

}

import sbt.*

object AppDependencies {
  private val mongoVersion: String = "2.6.0"
  private val bootstrapVersion: String = "9.13.0"
  private val enumeratumVersion = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-partials-play-30"      % "10.1.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "2.2.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.6.0",
    "com.beachape"      %% "enumeratum-play"            % enumeratumVersion,
    "org.julienrf"      %% "play-json-derived-codecs"   % "11.0.0",
    "org.apache.commons" % "commons-text"               % "1.13.1",
    "uk.gov.hmrc"       %% "crypto-json-play-30"        % "8.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"    % mongoVersion      % Test,
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"     % bootstrapVersion  % Test,
    "org.mockito"       %% "mockito-scala-scalatest"    % "1.17.37"         % Test,
    "org.scalamock"     %% "scalamock"                  % "6.0.0"           % Test
  )

}

import sbt.*

object AppDependencies {
  private val mongoVersion: String = "2.12.0"
  private val bootstrapVersion: String = "10.5.0"
  private val playVersion: String = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"         % mongoVersion,
    "uk.gov.hmrc"       %% s"play-partials-$playVersion"      % "10.2.0",
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVersion" % "12.29.0",
    "uk.gov.hmrc"       %% s"crypto-json-$playVersion"        % "8.4.0",
    "uk.gov.hmrc"       %% s"domain-$playVersion"             % "11.0.0",
    "com.beachape"      %% "enumeratum-play"                  % "1.9.0",
    "org.julienrf"      %% "play-json-derived-codecs"         % "11.0.0",
    "org.apache.commons" % "commons-text"                     % "1.14.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % mongoVersion,
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"       %% "mockito-scala-scalatest"       % "2.0.0",
    "org.scalamock"     %% "scalamock"                     % "7.5.0"
  ).map(_ % Test)

}

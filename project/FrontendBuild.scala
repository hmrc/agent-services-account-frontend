import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "agent-services-account-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  lazy val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "8.10.0",
    "uk.gov.hmrc" %% "auth-client" % "2.3.0",
    "uk.gov.hmrc" %% "play-partials" % "6.1.0",
    "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "2.4.0",
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.5.0",
    "de.threedimensions" %% "metrics-play" % "2.5.13"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "3.0.4" % scope,
    "org.mockito" % "mockito-core" % "2.11.0" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
    "com.github.tomakehurst" % "wiremock" % "2.10.1" % scope
  )

}

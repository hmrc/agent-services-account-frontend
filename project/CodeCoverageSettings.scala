import sbt.Keys.parallelExecution
import sbt.{Setting, Test}
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedFiles: Seq[String] = Seq(
    ".*.template",
    ".*CallOps.*",
    ".*EmailService.*",
    ".*ManageAccessPermissionsConfig.*",
    ".*AuthActions.*",
    ".*ClientAction.*",
    ".*AgentPermissionsConnector.*",
    ".*BetaInvite.*",
  )

  val settings: Seq[Setting[_]] = Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
      ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 81.00,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      Test / parallelExecution := false
  )
}


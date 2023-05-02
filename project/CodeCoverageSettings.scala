import sbt.Keys.parallelExecution
import sbt.{Setting, Test}
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedFiles: Seq[String] = Seq(
    ".*.template",
    ".*CallOps.*",
    ".*EmailService.*",
    ".*AuthActions.*",
    ".*ClientAction.*",
    ".*AgentPermissionsConnector.*",
    ".*BetaInvite.*",
  )

  val settings: Seq[Setting[_]] = Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
      ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 84.00,
      ScoverageKeys.coverageMinimumStmtPerFile := 66.00,
      ScoverageKeys.coverageMinimumBranchTotal := 50.00,
      ScoverageKeys.coverageMinimumBranchPerFile := 30.00,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      Test / parallelExecution := false
  )
}


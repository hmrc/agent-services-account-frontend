package uk.gov.hmrc.agentservicesaccount.connectors

import uk.gov.hmrc.play.events.monitoring.HttpErrorMonitor

trait GGRegistrationFEHttpMonitor extends HttpErrorMonitor {
  override val source = "agent-services-account-frontend"
}

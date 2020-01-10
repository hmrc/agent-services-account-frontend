package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named}
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.models.{SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationConnector @Inject()(@Named("agent-client-authorisation-baseUrl") baseUrl: URL, http: HttpClient)(implicit val metrics: Metrics)
  extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getSuspensionDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionDetails] =
    monitor("ConsumerAPI-Get-AgencySuspensionDetails-GET") {
      http
        .GET[HttpResponse](s"$baseUrl/agent-client-authorisation/agent/suspension-details")
        .map(response =>
          response.status match {
            case 200 => Json.parse(response.body).as[SuspensionDetails]
            case 204 => SuspensionDetails(suspensionStatus = false, None)
          })
    } recoverWith {
      case _: NotFoundException => Future failed SuspensionDetailsNotFound("No record found for this agent")
    }
}

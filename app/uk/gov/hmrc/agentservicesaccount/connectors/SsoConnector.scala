package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL
import javax.inject.{Inject, Singleton}

import com.google.inject.name.Named
import play.api.Logger
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class SsoConnector @Inject()(http: HttpGet, @Named("sso-baseUrl") baseUrl: URL) extends GGRegistrationFEHttpMonitor {

  def validateExternalDomain(domain: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = new URL(baseUrl, s"/sso/validate/domain/$domain")
    http.GET(url.toString)
      .map(_ => true)
      .recover {
        case e: BadRequestException => false
        case e: Exception =>
          Logger.error(s"Unable to validate domain $domain", e)
          false
      }
  }
}

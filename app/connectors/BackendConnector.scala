package connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.play.config.inject.DefaultServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import wiring.WSVerbs

import scala.concurrent.Future

@Singleton
class BackendConnector @Inject()(servicesConfig: DefaultServicesConfig, http: WSVerbs) {

  lazy val baseURL: String = servicesConfig.baseUrl("soft-drinks-industry-levy")
  lazy val serviceURL = "hello-world"

  def retrieveHelloWorld()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET[HttpResponse](s"$baseURL/$serviceURL")
  }

}

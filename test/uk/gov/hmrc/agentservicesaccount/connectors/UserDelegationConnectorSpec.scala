package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

class UserDelegationConnectorSpec extends UnitSpec with GuiceOneAppPerTest with WireMockSupport  {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.agent-services-account.port" -> wireMockPort,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false
      )

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val connector = new UserDelegationConnector(new URL(s"http://localhost:$wireMockPort"), app.injector.instanceOf[HttpGet])
  private implicit val hc = HeaderCarrier()



}

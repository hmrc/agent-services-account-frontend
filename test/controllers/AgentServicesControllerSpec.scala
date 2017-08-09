package controllers

import connectors.BackendConnector
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AgentServicesControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockConfig: Configuration = app.injector.instanceOf[Configuration]
  val backendConnector: BackendConnector = mock[BackendConnector]

  val controller = new AgentServicesController(messagesApi, backendConnector, mockConfig)

  "AgentServicesController" should {
    "return Status: OK Body and body should contain correct content" in {
      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) mustBe OK
      contentType(response).get mustBe HTML
      contentAsString(response) must include(messagesApi("agent.services.account.heading"))
      contentAsString(response) must include(messagesApi("agent.services.account.heading.summary"))
      contentAsString(response) must include(messagesApi("agent.services.account.subHeading"))
      contentAsString(response) must include(messagesApi("agent.services.account.subHeading.summary"))
      contentAsString(response) must include("ARN123098-12")
    }
  }
}



package controllers

import javax.inject._

import auth.AuthActions
import connectors.BackendConnector
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class AgentServicesController @Inject()(val messagesApi: MessagesApi,
                                        implicit val configuration: Configuration,
                                        backendConnector: BackendConnector,
                                        authActions: AuthActions) extends FrontendController with I18nSupport {

  val root: Action[AnyContent] = authActions.AuthorisedWithAgentAsync {
    implicit request => Future successful Ok(views.html.pages.agent_services_homepage())
  }

}

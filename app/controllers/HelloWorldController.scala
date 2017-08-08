package controllers

import javax.inject._

import connectors.BackendConnector
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}

@Singleton
class HelloWorldController @Inject()(val messagesApi: MessagesApi, backendConnector: BackendConnector,
                                     implicit val configuration: Configuration) extends Controller with I18nSupport {

  def helloWorld(): Action[AnyContent] = Action {
    implicit request => Ok(views.html.pages.hello_world())
  }

}

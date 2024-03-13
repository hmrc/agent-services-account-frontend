package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.existing_supervisory_body
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class AmlsExistingSupervisoryBodyController @Inject()(agentAssuranceConnector: AgentAssuranceConnector,
                                                      actions: Actions,
                                                      existing_supervisory_body: existing_supervisory_body)(implicit val appConfig: AppConfig,
                                                      ec: ExecutionContext,
                                                      val cc: MessagesControllerComponents) extends FrontendController() {

  def showExistingSupervisoryBody: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody){

    }
  }


}

/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.PayeSubscriptionConnector
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.controllers.paye.{routes => payeRoutes}
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.paye_check_your_answers
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.paye_submitted
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
final class PayeSubscriptionRequestController @Inject() (
  actions: Actions,
  payeConnector: PayeSubscriptionConnector,
  cyaView: paye_check_your_answers,
  submittedView: paye_submitted,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  def showConfirm: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    payeConnector
      .getStatus()
      .flatMap { st =>
        val eligible = !st.hasSubscription && !st.hasRequestInProgress
        if (!eligible) {
          Future.successful(Redirect(routes.AgentServicesController.showAgentServicesAccount()))
        }
        else {
          payeConnector.getCyaData().map { data =>
//            TODO: Pull this out into separate method
            val addressHtml = Seq(
              Some(data.address.line1),
              Some(data.address.line2),
              data.address.line3,
              data.address.line4,
              Some(data.address.postCode)
            ).flatten.mkString("<br>")
            val rows = Seq(
              SummaryListData(
                "paye.cya.agentName",
                data.agentName,
                link = None
              ),
              SummaryListData(
                "paye.cya.contactName",
                data.contactName,
                link = None
              ),
              SummaryListData(
                "paye.cya.telephoneNumber",
                data.telephoneNumber.getOrElse(""),
                link = None
              ),
              SummaryListData(
                "paye.cya.emailAddress",
                data.emailAddress.getOrElse(""),
                link = None
              ),
              SummaryListData(
                "paye.cya.address",
                addressHtml,
                link = None
              )
            )
            Ok(cyaView(rows))
          }
        }
      }
  }

  def submit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    payeConnector
      .getStatus()
      .flatMap { st =>
        val eligible = !st.hasSubscription && !st.hasRequestInProgress
        if (!eligible) {
          Future.successful(Redirect(routes.AgentServicesController.showAgentServicesAccount()))
        }
        else {
          //  TODO: Implement correct call to this endpoint
          payeConnector
            .submitRequest()
            .map(_ => Redirect(payeRoutes.PayeSubscriptionRequestController.showSubmitted))
        }
      }
  }

  def showSubmitted: Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>
    Ok(submittedView())
  }

}

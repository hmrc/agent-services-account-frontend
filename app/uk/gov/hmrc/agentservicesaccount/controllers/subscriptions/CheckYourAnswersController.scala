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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.checkYourAnswersPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.{routes => subscriptionRoutes}
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionCyaData
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.{CT, SA}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.check_your_answers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject() (
  actions: Actions,
  agentServicesAccountConnector: AgentServicesAccountConnector,
  val sessionCacheService: SessionCacheService,
  checkYourAnswers: check_your_answers,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport {

  def showPage(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    withSubscriptionCyaData(request.subscriptionJourney) { data =>
      val summaryItems = buildSummaryListItems(data, legacyRegime)
      Future.successful(Ok(checkYourAnswers(summaryItems, legacyRegime)))
    }
  }

  def onSubmit(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    withSubscriptionCyaData(request.subscriptionJourney) { data =>
      val requestModel = data.toSubscriptionRequest(legacyRegime)
      agentServicesAccountConnector
        .submitLegacySubscriptionRequest(requestModel, legacyRegime)
        .map(_ => Redirect(getNextPage(currentPage = checkYourAnswersPage, legacyRegime = legacyRegime)))
    }
  }

  private def formatAddress(address: BusinessAddress): String = List(
    Some(address.addressLine1),
    address.addressLine2,
    address.addressLine3,
    address.addressLine4,
    address.postalCode,
    Some(address.countryCode)
  ).flatten.map(play.twirl.api.HtmlFormat.escape)
    .map(_.body)
    .mkString("<br/>")

  private[subscriptions] def buildSummaryListItems(
    data: SubscriptionCyaData,
    legacyRegime: LegacyRegime
  ): Seq[SummaryListData] = {
    Seq(
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.business-name",
        value = data.businessName,
        link = Some(subscriptionRoutes.UpdateBusinessNameController.showPage(legacyRegime))
      ),
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.phone-number",
        value = data.phoneNumber,
        link = Some(subscriptionRoutes.UpdatePhoneNumberController.showPage(legacyRegime))
      ),
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.email",
        value = data.email,
        link = Some(subscriptionRoutes.UpdateEmailAddressController.showPage(legacyRegime))
      ),
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.address",
        value = formatAddress(data.address),
        link = Some(subscriptionRoutes.UpdateAddressController.showPage(legacyRegime))
      )
    )
  }

  private def withSubscriptionCyaData(
    journey: SubscriptionJourney
  )(f: SubscriptionCyaData => Future[Result]): Future[Result] =
    (journey: Option[SubscriptionCyaData]) match {
      case Some(data) => f(data)
      case None =>
        Future.successful(
          BadRequest("[CheckYourAnswersController] missing CT CYA data")
        )
    }

}

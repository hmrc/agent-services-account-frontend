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
import uk.gov.hmrc.agentservicesaccount.actions.CtJourney
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.checkYourAnswersPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.{routes => subscriptionRoutes}
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtCyaData
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtSubscriptionRequest
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionAddress
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.ct_check_your_answers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtCheckYourAnswersController @Inject() (
  actions: Actions,
  agentServicesAccountConnector: AgentServicesAccountConnector,
  val sessionCacheService: SessionCacheService,
  checkYourAnswers: ct_check_your_answers,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    withCtCyaData(request.ctSubscriptionJourney) { data =>
      val summaryItems = buildSummaryListItems(data)
      Future.successful(Ok(checkYourAnswers(summaryItems)))
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    withCtCyaData(request.ctSubscriptionJourney) { data =>
      val requestModel = data.toSubscriptionRequest

      agentServicesAccountConnector
        .submitCtRequest(requestModel)
        .map(_ => Redirect(getNextPage(checkYourAnswersPage)))
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

  private[subscriptions] def buildSummaryListItems(data: CtCyaData): Seq[SummaryListData] = Seq(
    SummaryListData(
      key = "asa.legacy.ct.check-your-answers.business-name",
      value = data.agencyName,
      link = Some(subscriptionRoutes.CtUpdateBusinessNameController.showPage)
    ),
    SummaryListData(
      key = "asa.legacy.ct.check-your-answers.phone-number",
      value = data.agencyTelephone,
      link = Some(subscriptionRoutes.CtUpdatePhoneNumberController.showPage)
    ),
    SummaryListData(
      key = "asa.legacy.ct.check-your-answers.email",
      value = data.agencyEmail,
      link = Some(subscriptionRoutes.CtUpdateEmailAddressController.showPage)
    ),
    SummaryListData(
      key = "asa.legacy.ct.check-your-answers.address",
      value = formatAddress(data.agencyAddress),
      link = Some(subscriptionRoutes.CtUpdateAddressController.showPage)
    )
  )

//  TODO: 10906 Make this an implicit conversion on the model class
  private def getCtCyaData(journey: CtJourney): Option[CtCyaData] =
    for {
      businessName <-
        journey.useCustomBusinessName match {
          case Some(true) => journey.businessNameAnswer
          case _ => journey.asaDetails.agencyName
        }
      phoneNumber <-
        journey.useCustomPhoneNumber match {
          case Some(true) => journey.phoneNumberAnswer
          case _ => journey.asaDetails.agencyTelephone
        }
      email <-
        journey.useCustomEmail match {
          case Some(true) => journey.emailAnswer
          case _ => journey.asaDetails.agencyEmail
        }
      address <-
        journey.useCustomAddress match {
          case Some(true) => journey.addressAnswer
          case _ => journey.asaDetails.agencyAddress
        }
    } yield CtCyaData(
      agencyName = businessName,
      agencyEmail = email,
      agencyTelephone = phoneNumber,
      agencyAddress = address
    )

  private def withCtCyaData(
    journey: CtJourney
  )(f: CtCyaData => Future[Result]): Future[Result] =
    getCtCyaData(journey) match {
      case Some(data) => f(data)
      case None =>
        Future.successful(
          BadRequest("[CtCheckYourAnswersController] missing CT CYA data")
        )
    }

}

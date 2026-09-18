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
import play.api.mvc.*
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.SubscriptionJourneyRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.checkYourAnswersPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.routes as subscriptionRoutes
import uk.gov.hmrc.agentservicesaccount.controllers.routes as asaRoutes
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.CT_SA_EMAIL_MAX_LENGTH
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionCyaData
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.SA
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionCyaData.subscriptionJourneyToCyaData
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.CountryResolver
import uk.gov.hmrc.agentservicesaccount.utils.RequestAwareLogging
import uk.gov.hmrc.agentservicesaccount.utils.SanitiseLegacySubscriptionName
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
  countryResolver: CountryResolver,
  checkYourAnswers: check_your_answers,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with RequestAwareLogging {

  def showPage(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    withSubscriptionCyaData(request, agentRegime) { data =>
      val summaryItems = buildSummaryListItems(
        data,
        agentRegime,
        request.subscriptionJourney.asaDetails.agencyEmail.map(_.length),
        request.subscriptionJourney.useCustomAddress
      )
      Future.successful(Ok(checkYourAnswers(summaryItems, agentRegime)))
    }
  }

  def onSubmit(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val isWelsh = messagesApi.preferred(request).lang.code == "cy"
    withSubscriptionCyaData(request, agentRegime) { data =>
      val requestModelOpt =
        if (agentRegime == PAYE) {
          request.subscriptionJourney.asaDetails.agencyName.flatMap(asaAgencyName => {
            val sanitised = SanitiseLegacySubscriptionName.sanitise(asaAgencyName, agentRegime)
            if (sanitised.removedCharacters.nonEmpty) {
              logger.warn(s"[subscriptions][CheckYourAnswersController][onSubmit] - Remove invalid characters ${sanitised.removedCharacters.mkString} from ASA agency name for ARN ${request.agentInfo.arn.value} and agent regime $agentRegime")
            }
            data.toSubscriptionRequest(
              agentRegime,
              isWelsh,
              asaAgentNameOpt = Some(sanitised.sanitisedName)
            )
          })
        }
        else {
          val sanitised = SanitiseLegacySubscriptionName.sanitise(data.name, agentRegime)
          if (sanitised.removedCharacters.nonEmpty) {
            logger.warn(s"[subscriptions][CheckYourAnswersController][onSubmit] - Remove invalid characters ${sanitised.removedCharacters.mkString} from ASA agency name for ARN ${request.agentInfo.arn.value} and agent regime $agentRegime")
          }
          val dataWithSanitisedName = data.copy(name = sanitised.sanitisedName)
          dataWithSanitisedName.toSubscriptionRequest(
            agentRegime,
            isWelsh,
            countryNameOpt = Some(countryResolver.countryName(dataWithSanitisedName.address.countryCode, checkLengthForSubmission = true))
          )
        }

      requestModelOpt.map(requestModel => {
        for {
          _ <- agentServicesAccountConnector.submitSubscriptionRequest(requestModel, agentRegime)
          updatedJourney = request.subscriptionJourney.copy(isSubmitted = true)
          _ <- sessionCacheService.put(subscriptionJourneyKey(agentRegime), updatedJourney)
        } yield Redirect(getNextPage(
          checkYourAnswersPage,
          Some(updatedJourney),
          agentRegime
        ))
      }).getOrElse(Future.successful(Redirect(routes.CheckYourAnswersController.showPage(agentRegime))))
    }
  }

  private def formatAddress(address: BusinessAddress): String = List(
    Some(address.addressLine1),
    address.addressLine2,
    address.addressLine3,
    address.addressLine4,
    address.postalCode,
    Some(countryResolver.countryName(address.countryCode))
  ).flatten.map(play.twirl.api.HtmlFormat.escape)
    .map(_.body)
    .mkString("<br/>")

  private[subscriptions] def buildSummaryListItems(
    data: SubscriptionCyaData,
    agentRegime: AgentRegime,
    agencyDetailsEmailLength: Option[Int],
    useCustomAddress: Option[Boolean]
  ): Seq[SummaryListData] = {
    val nameRowKeyDescriptor =
      if (agentRegime == PAYE)
        "contact"
      else
        "business"
    val nameRowKey = s"${agentRegime.msgPrefix}.check-your-answers.$nameRowKeyDescriptor-name"
    val nameRowLink =
      if (agentRegime == PAYE) {
        Some(subscriptionRoutes.PayeUpdateContactNameController.showPage)
      }
      else {
        Some(subscriptionRoutes.UpdateBusinessNameController.showPage(agentRegime))
      }
    val emailAddressLink =
      (agentRegime, agencyDetailsEmailLength) match {
        case (CT | SA, Some(length)) if length > CT_SA_EMAIL_MAX_LENGTH => Some(subscriptionRoutes.UpdateEmailAddressController.showSaCtCustomPage(agentRegime))
        case _ => Some(subscriptionRoutes.UpdateEmailAddressController.showPage(agentRegime))
      }
    Seq(
      SummaryListData(
        key = nameRowKey,
        value = data.name,
        link = nameRowLink
      ),
      SummaryListData(
        key = s"${agentRegime.msgPrefix}.check-your-answers.phone-number",
        value = data.phoneNumber,
        link = Some(subscriptionRoutes.UpdatePhoneNumberController.showPage(agentRegime))
      ),
      SummaryListData(
        key = s"${agentRegime.msgPrefix}.check-your-answers.email",
        value = data.email,
        link = emailAddressLink
      ),
      SummaryListData(
        key = s"${agentRegime.msgPrefix}.check-your-answers.address",
        value = formatAddress(data.address),
        link = Some(if (useCustomAddress.contains(true))
          subscriptionRoutes.UpdateAddressController.showChange(agentRegime, isInvalid = false)
        else
          subscriptionRoutes.UpdateAddressController.showPage(agentRegime))
      )
    )
  }

  private def withSubscriptionCyaData(
    request: SubscriptionJourneyRequest[AnyContent],
    agentRegime: AgentRegime
  )(f: SubscriptionCyaData => Future[Result]): Future[Result] = {
    val journey = request.subscriptionJourney
    (subscriptionJourneyToCyaData(journey, agentRegime): Option[SubscriptionCyaData]) match {
      case Some(data) if !journey.isSubmitted => f(data)
      case _ if journey.isSubmitted => Future.successful(Redirect(subscriptionRoutes.ConfirmationController.showConfirmationPage(agentRegime)))
      case _ =>
        logger.warn("[CheckYourAnswersController] missing Legacy Subscription CYA data")(using request)
        Future.successful(Redirect(asaRoutes.AgentServicesController.showAgentServicesAccount()))
    }
  }

}

/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util

import play.api.mvc.Call
import uk.gov.hmrc.agentservicesaccount.controllers.routes as homeRoutes
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.CT_SA_EMAIL_MAX_LENGTH
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.SA
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.STT

object NextPageSelector {

  val rootPage = "home"
  val doYouAlreadyManagePage = "doYouAlreadyManage"
  val youMayNotNeedToApplyPage = "youMayNotNeedToApply"
//  TODO: 12614 Rename this
  val payeUpdateContactNamePage = "payeContactName"
  val updateBusinessNamePage = "businessName"
  val updatePhoneNumberPage = "phoneNumber"
  val updateEmailAddressPage = "emailAddress"
  val emailVerificationFinish = "emailVerificationFinish"
  val updateAddressPage = "address"
  val changeAddressPage = "changeAddress"
  val addressLookupFinish = "addressLookupFinish"
  val checkYourAnswersPage = "checkYourAnswers"

  private val nextPage: (
    String,
    Option[SubscriptionJourney],
    AgentRegime
  ) => Call = {
    case (_, Some(journey), regime) if journey.isSubmitted => subscriptions.routes.ConfirmationController.showConfirmationPage(regime)
    case (_, Some(journey), regime) if journey.isComplete(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
    case (`rootPage`, _, regime) =>
      regime match {
//        TODO: 12614 After modifying PayeUpdateContactNameController to be non PAYE-specific, modify this route accordingly
        case STT => subscriptions.routes.UpdateContactNameController.showPage(PAYE)
        case _ => subscriptions.routes.DoYouAlreadyManageController.showPage(regime)
      }
    case (`doYouAlreadyManagePage`, Some(journey), regime) =>
      (journey.doYouAlreadyManage, regime) match {
        case (Some(true), _) => subscriptions.routes.YouMayNotNeedToApplyController.showPage(regime)
        case (Some(false), PAYE) => subscriptions.routes.UpdateContactNameController.showPage(PAYE)
        case (Some(false), _) => subscriptions.routes.UpdateBusinessNameController.showPage(regime)
        case _ => subscriptions.routes.DoYouAlreadyManageController.showPage(regime)
      }
    case (`youMayNotNeedToApplyPage`, Some(journey), regime) =>
      (journey.youMayNotNeedToApply, regime) match {
        case (Some(true), PAYE) => subscriptions.routes.UpdateContactNameController.showPage(PAYE)
        case (Some(true), regime) => subscriptions.routes.UpdateBusinessNameController.showPage(regime)
        case (Some(false), _) => homeRoutes.AgentServicesController.root()
        case _ => subscriptions.routes.YouMayNotNeedToApplyController.showPage(regime)
      }
    case (`payeUpdateContactNamePage`, _, PAYE) => subscriptions.routes.UpdatePhoneNumberController.showPage(PAYE)
    case (`updateBusinessNamePage`, _, regime) => subscriptions.routes.UpdatePhoneNumberController.showPage(regime)
    case (`updatePhoneNumberPage`, _, regime) => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
    case (`updateEmailAddressPage`, Some(journey), regime) =>
      (journey.useCustomEmail, regime, journey.asaDetails.agencyEmail.map(_.length)) match {
        case (Some(false), CT | SA, Some(length)) if length > CT_SA_EMAIL_MAX_LENGTH =>
          subscriptions.routes.UpdateEmailAddressController.showSaCtCustomPage(regime)
        case (Some(false), _, _) => subscriptions.routes.UpdateAddressController.showPage(regime)
        case _ => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
      }
    case (`emailVerificationFinish`, _, regime) => subscriptions.routes.UpdateAddressController.showPage(regime)
    case (`updateAddressPage`, Some(journey), regime) =>
      journey.useCustomAddress match {
        case Some(false) if journey.addressValidForRegime(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case Some(false) => subscriptions.routes.UpdateAddressController.showChange(regime, isInvalid = true)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`addressLookupFinish`, Some(journey), regime) =>
      (journey.useCustomAddress, journey.addressAnswer) match {
        case (Some(true), Some(_)) if journey.addressValidForRegime(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case (Some(true), Some(_)) => subscriptions.routes.UpdateAddressController.showChange(regime, isInvalid = true)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`changeAddressPage`, _, regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
    case (_, _, _) => homeRoutes.AgentServicesController.root()
  }

  def getNextPage(
    currentPage: String,
    journey: Option[SubscriptionJourney] = None,
    agentRegime: AgentRegime
  ): Call = {
    nextPage(
      currentPage,
      journey,
      agentRegime
    )
  }

}

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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesiDetailsJourney
object NextPageSelector {

  def getNextPage(
    journey: DesiDetailsJourney,
    currentPage: String
  ): Result = {
    if (journey.journeyComplete) {
      Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
    }
    else {
      val nextPage: Option[String] = journey.contactChangesNeeded.flatMap { pages =>
        if (pages.contains(currentPage)) {
          pages.toSeq.sliding(2).find {
            case Seq(current, _) => current == currentPage
            case _ => false
          }.flatMap {
            case Seq(_, next) => Some(next)
            case _ => None
          }
        }
        else
          pages.headOption
      }
      nextPage match {
        case Some("businessName") => Redirect(desiDetails.routes.UpdateNameController.showPage)
        case Some("address") => Redirect(desiDetails.routes.ContactDetailsController.startAddressLookup)
        case Some("email") => Redirect(desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress)
        case Some("telephone") => Redirect(desiDetails.routes.UpdateTelephoneController.showPage)
        case None => Redirect(desiDetails.routes.ApplySACodeChangesController.showPage)
      }
    }
  }
}

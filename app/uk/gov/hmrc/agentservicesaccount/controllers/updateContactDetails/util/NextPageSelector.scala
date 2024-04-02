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

package uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util

import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.controllers._

import scala.concurrent.{ExecutionContext, Future}

object NextPageSelector {

  //ToDo: Use enums
  def getNextPage(sessionCache: SessionCacheService, currentPage: String = "selectChanges")(implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    for {
      currentSelectedChanges <- sessionCache.get(CURRENT_SELECTED_CHANGES)
      previousSelectedChanges <- sessionCache.get(PREVIOUS_SELECTED_CHANGES)
      pagesRequired = {
        for {
          current <- currentSelectedChanges
          previous <- previousSelectedChanges
        } yield current.diff(previous)
      }
      nextPage = getPage(pagesRequired, currentPage, false)
    } yield nextPage
  }

  //TODO: use enums
  private def getPage(pagesRequired: Option[Set[String]], currentPage: String, userIsOnCheckYourAnswersFlow: Boolean): Result = {
    val nextPage: Option[String] = pagesRequired.flatMap { pages =>
      if (pages.contains(currentPage)) {
        pages.toSeq.sliding(2).find {
          case Seq(current, _) => current == currentPage
          case _ => false
        }.flatMap {
          case Seq(_, next) => Some(next)
          case _ => None
        }
      } else pages.headOption
    }

    nextPage match {
      case Some("businessName") => Redirect(updateContactDetails.routes.ContactDetailsController.showChangeBusinessName)
      case Some("address") => Redirect(updateContactDetails.routes.ContactDetailsController.showChangeEmailAddress) //TODO: Update routing
      case Some("email") => Redirect(updateContactDetails.routes.ContactDetailsController.showChangeEmailAddress)
      case Some("telephone") => Redirect(updateContactDetails.routes.ContactDetailsController.showChangeTelephoneNumber)
      case None => {
        if (userIsOnCheckYourAnswersFlow) Redirect(updateContactDetails.routes.ContactDetailsController.showCheckNewDetails)
        else Redirect(desiDetails.routes.ApplySACodeChanges.showPage)
      }
    }
  }
}

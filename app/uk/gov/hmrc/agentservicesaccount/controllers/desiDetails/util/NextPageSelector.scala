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

import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, PREVIOUS_SELECTED_CHANGES, desiDetails}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import scala.concurrent.{ExecutionContext, Future}

object NextPageSelector {

  def moveToCheckYourAnswersFlow(sessionCache: SessionCacheService)(implicit request: Request[_], ec: ExecutionContext): Future[(String, String)] = {
    for {
      currentSelectedPages <- sessionCache.get(CURRENT_SELECTED_CHANGES)
      selectedPages: Set[String] = currentSelectedPages.getOrElse(Set.empty)
      previousSelectedPages <- sessionCache.put(PREVIOUS_SELECTED_CHANGES, selectedPages)
    } yield previousSelectedPages
  }

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
      nextPage = getPage(pagesRequired, currentPage, previousSelectedChanges.getOrElse(Set.empty).nonEmpty)
    } yield nextPage
  }

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
      case Some("businessName") => Redirect(desiDetails.routes.UpdateNameController.showPage)
      case Some("address") => Redirect(desiDetails.routes.ContactDetailsController.startAddressLookup)
      case Some("email") => Redirect(desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress)
      case Some("telephone") => Redirect(desiDetails.routes.UpdateTelephoneController.showPage)
      case None => {
        if (userIsOnCheckYourAnswersFlow) Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
        else Redirect(desiDetails.routes.ApplySACodeChangesController.showPage)
      }
    }
  }
}

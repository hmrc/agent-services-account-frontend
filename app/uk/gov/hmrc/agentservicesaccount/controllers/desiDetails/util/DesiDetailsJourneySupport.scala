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

import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Redirect
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesiDetailsJourney
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait DesiDetailsJourneySupport {

  implicit val ec: ExecutionContext

  val sessionCacheService: SessionCacheService

  def withUpdateDesiDetailsJourney(body: DesignatoryDetails => Future[Result])(
    implicit request: RequestHeader
  ): Future[Result] = {
    sessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey).flatMap {
      case Some(desiDetails: DesignatoryDetails) => body(desiDetails)
      case None => Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

  def ifChangeContactDetailsFeatureEnabled(action: => Future[Result])(implicit appConfig: AppConfig): Future[Result] = {
    if (appConfig.enableChangeContactDetails)
      action
    else
      Future.successful(NotFound)
  }

  def ifChangeContactFeatureEnabledAndNoPendingChanges(action: => Future[Result])(implicit
    request: AuthRequestWithAgentInfo[_],
    appConfig: AppConfig,
    pcodRepository: PendingChangeRequestRepository
  ): Future[Result] = ifChangeContactDetailsFeatureEnabled {
    pcodRepository.find(request.agentInfo.arn).flatMap {
      case None => // no change is pending, we can proceed
        action
      case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
        Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
    }
  }

  def contactChangesNeeded()(implicit request: AuthRequestWithAgentInfo[_]): Future[Option[Set[String]]] = {
    for {
      selectChanges <- sessionCacheService.get[Set[String]](currentSelectedChangesKey)
      desiDetailsData <- sessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)
    } yield desiDetailsData match {
      case Some(details) => {
        val detailsUpdated: Map[String, Boolean] = Map(
          "businessName" -> details.agencyDetails.agencyName.isEmpty,
          "address" -> details.agencyDetails.agencyAddress.isEmpty,
          "email" -> details.agencyDetails.agencyEmail.isEmpty,
          "telephone" -> details.agencyDetails.agencyTelephone.isEmpty
        )

        selectChanges.map(changes => changes.filter(change => detailsUpdated(change)))
      }
      case _ => {
        selectChanges
      }
    }
  }

  def isContactPageRequestValid(currentPage: String)(
    implicit request: AuthRequestWithAgentInfo[_]
  ): Future[Boolean] = {
    sessionCacheService.get[Set[String]](currentSelectedChangesKey).map { selectChanges =>
      selectChanges.fold(false)(changes => changes.contains(currentPage))
    }
  }

  def isOtherServicesPageRequestValid()(
    implicit request: AuthRequestWithAgentInfo[_]
  ): Future[Boolean] = {
    for {
      selectChanges <- sessionCacheService.get[Set[String]](currentSelectedChangesKey)
      changesStillNeeded <- contactChangesNeeded()
    } yield selectChanges.isDefined && changesStillNeeded.isEmpty || changesStillNeeded.contains(Set.empty)
  }

  def isJourneyComplete()(
    implicit request: AuthRequestWithAgentInfo[_]
  ): Future[DesiDetailsJourney] = {
    for {
      submittedBy <- sessionCacheService.get[YourDetails](draftSubmittedByKey)
      desiDetailsData <- sessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)
      unchangedDetails <- contactChangesNeeded()
    } yield desiDetailsData match {
      case Some(details) => {
        (unchangedDetails, submittedBy, details.otherServices) match {
          case (Some(changes: Set[String]), Some(_: YourDetails), _: OtherServices) if changes.isEmpty => DesiDetailsJourney(None, journeyComplete = true)
          case (None, Some(_: YourDetails), _: OtherServices) => DesiDetailsJourney(None, journeyComplete = true)
          case _ => DesiDetailsJourney(unchangedDetails, journeyComplete = false)
        }
      }
      case _ => DesiDetailsJourney(unchangedDetails, journeyComplete = false)
    }
  }

}

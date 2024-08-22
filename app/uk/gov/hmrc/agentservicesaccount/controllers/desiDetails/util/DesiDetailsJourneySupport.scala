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

import play.api.mvc.Results.{NotFound, Redirect}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY, desiDetails, routes}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesiDetailsJourney, DesignatoryDetails, OtherServices, YourDetails}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait DesiDetailsJourneySupport {

  def sessionCache: SessionCacheService

  def withUpdateDesiDetailsJourney(body: DesignatoryDetails => Future[Result])(
    implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).flatMap {
      case Some(desiDetails: DesignatoryDetails) => body(desiDetails)
      case None =>
        Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

  def ifChangeContactDetailsFeatureEnabled(action: => Future[Result])(implicit appConfig: AppConfig): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  def ifChangeContactFeatureEnabledAndNoPendingChanges(action: => Future[Result])
                                                      (implicit request: AuthRequestWithAgentInfo[_],
                                                       appConfig: AppConfig,
                                                       pcodRepository: PendingChangeRequestRepository,
                                                       ec: ExecutionContext,
                                                       hc: HeaderCarrier): Future[Result] =
    ifChangeContactDetailsFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
      }
    }

  def contactChangesNeeded()(implicit request: AuthRequestWithAgentInfo[_],
                             agentAssuranceConnector: AgentAssuranceConnector,
                             hc: HeaderCarrier,
                             ec: ExecutionContext): Future[Option[Set[String]]] = {
    for {
      selectChanges <- sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES)
      desiDetailsData <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
      oldContactDetails <- agentAssuranceConnector.getAgentRecord.map(_.agencyDetails.getOrElse {
        throw new RuntimeException(s"Could not retrieve current agency details for ${request.agentInfo.arn} from the backend")
      })
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
    implicit request: AuthRequestWithAgentInfo[_], ec: ExecutionContext
  ): Future[Boolean] = {
    sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES).map { selectChanges =>
      selectChanges.fold(false)(changes => changes.contains(currentPage))
    }
  }

  def isOtherServicesPageRequestValid()(
    implicit
    request: AuthRequestWithAgentInfo[_],
    ec: ExecutionContext,
    agentAssuranceConnector: AgentAssuranceConnector,
    hc: HeaderCarrier
  ): Future[Boolean] = {
    for {
      selectChanges <- sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES)
      changesStillNeeded <- contactChangesNeeded()
    } yield selectChanges.isDefined && changesStillNeeded.isEmpty || changesStillNeeded.contains(Set.empty)
  }

  def isJourneyComplete()(
    implicit request: AuthRequestWithAgentInfo[_], agentAssuranceConnector: AgentAssuranceConnector, hc: HeaderCarrier, ec: ExecutionContext
  ): Future[DesiDetailsJourney] = {
    for {
      submittedBy <- sessionCache.get[YourDetails](DRAFT_SUBMITTED_BY)
      desiDetailsData <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
      unchangedDetails <- contactChangesNeeded()
    } yield desiDetailsData match {
      case Some(details) => {
        (unchangedDetails, submittedBy, details.otherServices) match {
          case (Some(changes: Set[String]), Some(_: YourDetails), _: OtherServices) if changes.isEmpty =>
            DesiDetailsJourney(None, journeyComplete = true)
          case (None, Some(_: YourDetails), _: OtherServices) =>
            DesiDetailsJourney(None, journeyComplete = true)
          case _ =>
            DesiDetailsJourney(unchangedDetails, journeyComplete = false)
        }
      }
      case _ => DesiDetailsJourney(unchangedDetails, journeyComplete = false)
    }
  }

}

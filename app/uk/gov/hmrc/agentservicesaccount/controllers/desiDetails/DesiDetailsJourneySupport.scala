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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import play.api.mvc.Results.{NotFound, Redirect}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.controllers.{DRAFT_NEW_CONTACT_DETAILS, desiDetails, routes}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait DesiDetailsJourneySupport {

  def sessionCache: SessionCacheService
  def acaConnector:AgentClientAuthorisationConnector

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HcProvider.headerCarrier

  private object HcProvider extends FrontendHeaderCarrierProvider {
    def headerCarrier(implicit request: Request[_]): HeaderCarrier = hc(request)
  }

  def updateDraftDetails(f: DesignatoryDetails => DesignatoryDetails)(implicit request: Request[_], ec: ExecutionContext): Future[Unit] = for {
    mDraftDetailsInSession <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
    draftDetails <- mDraftDetailsInSession match {
      case Some(details) => Future.successful(details)
      // if there is no 'draft' new set of details in session, get a fresh copy of the current stored details
      case None =>
        acaConnector.getAgencyDetails()
          .map(_.getOrElse(throw new RuntimeException("Current agency details are unavailable")))
          .map(agencyDetails=>
            DesignatoryDetails(
              agencyDetails = agencyDetails,
              otherServices = OtherServices(
                saChanges = SaChanges(
                  applyChanges = false,
                  saAgentReference = None),
                ctChanges = CtChanges(
                  applyChanges = false,
                  ctAgentReference = None
                ))))
    }
    updatedDraftDetails = f(draftDetails)
    _ <- sessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, updatedDraftDetails)
  } yield ()

  def withUpdateDesiDetailsJourney(body: DesignatoryDetails => Future[Result])(
    implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).flatMap{
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
                                          pcodRepository: PendingChangeOfDetailsRepository,
                                          ec: ExecutionContext): Future[Result] = {
    ifChangeContactDetailsFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(desiDetails.routes.ContactDetailsController.showCurrentContactDetails))
      }
    }
  }

}

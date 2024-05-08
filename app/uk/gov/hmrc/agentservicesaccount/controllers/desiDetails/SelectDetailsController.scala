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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, DRAFT_NEW_CONTACT_DETAILS, ToFuture}
import uk.gov.hmrc.agentservicesaccount.forms.SelectChangesForm
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesignatoryDetails, SelectChanges}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.select_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectDetailsController @Inject()(actions: Actions,
                                        val sessionCache: SessionCacheService,
                                        select_changes_view: select_changes
                                       )(implicit appConfig: AppConfig,
                                         cc: MessagesControllerComponents,
                                         ec: ExecutionContext,
                                         pcodRepository: PendingChangeRequestRepository,
                                         agentAssuranceConnector: AgentAssuranceConnector
                                       ) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport with Logging {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES).map {
        case Some(data) => {
          val savedAnswers: SelectChanges =
            SelectChanges(
              businessName = Some("businessName").filter(data.contains),
              address = Some("address").filter(data.contains),
              email = Some("email").filter(data.contains),
              telephone = Some("telephone").filter(data.contains)
            )
          Ok(select_changes_view(SelectChangesForm.form.fill(savedAnswers)))
        }
        case _ => Ok(select_changes_view(SelectChangesForm.form))
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      SelectChangesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            BadRequest(select_changes_view(formWithErrors)).toFuture
          },
          (selectedChanges: SelectChanges) => {

            sessionCache.put(CURRENT_SELECTED_CHANGES, selectedChanges.pagesSelected).flatMap {
              _ => {
                resetUncheckedAndNavigate(selectedChanges).flatten
              }
            }
          }
        )
    }
  }

  private def resetUncheckedAndNavigate(selectedChanges: SelectChanges)
                                       (implicit request: AuthRequestWithAgentInfo[AnyContent]): Future[Future[Result]] = {
    for {
      desiDetailsData <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
      oldContactDetails <- agentAssuranceConnector.getAgentRecord.map(_.agencyDetails.getOrElse {
        throw new RuntimeException(s"Could not retrieve current agency details for ${request.agentInfo.arn} from the backend")
      })
      journey <- isJourneyComplete()
    } yield desiDetailsData match {
      case Some(newData) =>
        sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, newData.copy(agencyDetails = oldContactDetails.copy(
          agencyName = if(selectedChanges.businessName.isDefined) newData.agencyDetails.agencyName else None,
          agencyAddress = if(selectedChanges.address.isDefined) newData.agencyDetails.agencyAddress else None,
          agencyEmail = if(selectedChanges.email.isDefined) newData.agencyDetails.agencyEmail else None,
          agencyTelephone = if(selectedChanges.telephone.isDefined) newData.agencyDetails.agencyTelephone else None
        ))).flatMap(_ => Future.successful(getNextPage(journey, "selectChanges")))
      case _ => Future.successful(getNextPage(journey, "selectChanges"))
    }
  }
}

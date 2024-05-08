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
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.DRAFT_NEW_CONTACT_DETAILS
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateTelephoneController @Inject()(actions: Actions,
                                          val sessionCache: SessionCacheService,
                                          draftDetailsService: DraftDetailsService,
                                          update_phone: update_phone
                                         )(implicit appConfig: AppConfig,
                                           cc: MessagesControllerComponents,
                                           ec: ExecutionContext,
                                           pcodRepository: PendingChangeRequestRepository,
                                           agentAssuranceConnector: AgentAssuranceConnector
                                         ) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport with Logging {

  val showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      isContactPageRequestValid("telephone").flatMap {
        case true => sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).map {
          case Some(desiDetails) if desiDetails.agencyDetails.agencyTelephone.isDefined =>
            Ok(update_phone(UpdateDetailsForms.telephoneNumberForm.fill(desiDetails.agencyDetails.agencyTelephone.get)))
          case _ => Ok(update_phone(UpdateDetailsForms.telephoneNumberForm))
        }
        case _ => Future.successful(Redirect(routes.SelectDetailsController.showPage))
      }

    }
  }

  val onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.telephoneNumberForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(update_phone(formWithErrors))),
          newPhoneNumber => {
            draftDetailsService.updateDraftDetails(desiDetails =>
              desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyTelephone = Some(newPhoneNumber)))
            ).flatMap(_ =>
              isJourneyComplete().flatMap(journey => Future.successful(getNextPage(journey, "telephone")))
            )
          }
        )
    }
  }
}

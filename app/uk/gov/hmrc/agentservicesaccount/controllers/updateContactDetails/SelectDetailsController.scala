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

package uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, PREVIOUS_SELECTED_CHANGES, ToFuture}
import uk.gov.hmrc.agentservicesaccount.forms.SelectChangesForm
import uk.gov.hmrc.agentservicesaccount.models.SelectChanges
import uk.gov.hmrc.agentservicesaccount.repository.UpdateContactDetailsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details.select_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectDetailsController @Inject()(actions: Actions,
                                        val updateContactDetailsJourneyRepository: UpdateContactDetailsJourneyRepository,
                                        sessionCache: SessionCacheService,
                                        select_changes_view: select_changes
                                       )(implicit appConfig: AppConfig,
                                         cc: MessagesControllerComponents,
                                         ec: ExecutionContext
                                        ) extends FrontendController(cc)
                                          with UpdateContactDetailsJourneySupport with I18nSupport with Logging{

  def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      Ok(select_changes_view(SelectChangesForm.form)).toFuture
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      SelectChangesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Ok(select_changes_view(formWithErrors)).toFuture
          },
          (selectedChanges: SelectChanges) => {
            sessionCache.put(CURRENT_SELECTED_CHANGES, selectedChanges.pagesSelected).flatMap {
              _ => {
                removeUnchecked(selectedChanges)
                getNextPage(sessionCache)
              }
            }
          }
        )
    }
  }

  //ToDo: use enums
  //ToDo: Remove session data for unchecked
  def removeUnchecked(selectedChanges: SelectChanges)(implicit request: Request[_], ec: ExecutionContext): Unit = {
    // val allPages: Set[String] = Set("businessName", "address", "email", "telephone")
    // val unchecked: Set[String] = allPages.diff(selectedChanges.pagesSelected)
    for {
      previousPages <- sessionCache.get(PREVIOUS_SELECTED_CHANGES)
      checkedPreviousPages = selectedChanges.pagesSelected.intersect(previousPages.getOrElse(Set()))
    } yield sessionCache.put(PREVIOUS_SELECTED_CHANGES, checkedPreviousPages)
  }
}

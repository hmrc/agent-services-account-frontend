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
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ToFuture
import uk.gov.hmrc.agentservicesaccount.repository.UpdateContactDetailsJourneyRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SACodeController @Inject()(actions: Actions,
                                 val updateContactDetailsJourneyRepository: UpdateContactDetailsJourneyRepository,
                                       )(implicit appConfig: AppConfig,
                                         cc: MessagesControllerComponents) extends FrontendController(cc)
                                          with UpdateContactDetailsJourneySupport with I18nSupport with Logging{

  def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async {
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      Ok("").toFuture
    }
  }
}

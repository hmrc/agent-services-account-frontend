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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.models.AmlsRequest
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.services.AuditService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.check_your_answers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

@Singleton
class CheckYourAnswersController @Inject() (
  actions: Actions,
  agentAssuranceConnector: AgentAssuranceConnector,
  val sessionCacheService: SessionCacheService,
  checkYourAnswers: check_your_answers,
  cc: MessagesControllerComponents,
  auditService: AuditService
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with AmlsJourneySupport
with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { journeyData =>
        val model = buildSummaryListItems(
          journeyData.isUkAgent,
          journeyData,
          request.messages.lang.locale
        )
        Ok(checkYourAnswers(model)).toFuture
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {

      withUpdateAmlsJourney { journeyData =>
        (journeyData.newAmlsBody, journeyData.newRegistrationNumber) match {
          case (Some(newAmlsBody), Some(newRegistrationNumber)) =>
            val amlsRequest = AmlsRequest(
              journeyData.isUkAgent,
              newAmlsBody,
              newRegistrationNumber,
              journeyData.newExpirationDate
            )

            for {
              _ <- agentAssuranceConnector.postAmlsDetails(request.agentInfo.arn, amlsRequest)
              oldAmlsDetails <- Try {
                agentAssuranceConnector.getAMLSDetails(request.agentInfo.arn.value).map(Option(_))
              }.getOrElse(Future.successful(None))
              optUtr <- Try {
                agentAssuranceConnector.getAgentRecord.map(_.uniqueTaxReference)
              }.getOrElse(Future.successful(None))
              _ = auditService.auditUpdateAmlSupervisionDetails(
                amlsRequest,
                oldAmlsDetails,
                request.agentInfo.arn,
                optUtr
              )
            } yield Redirect(amls.routes.AmlsConfirmationController.showUpdatedAmlsConfirmationPage(journeyData.hasExistingAmls))
          case (optNewAmlsBody, optNewRegistrationNumber) =>
            Future.successful(BadRequest(s"[checkYourAnswersController][onSubmit] missing mandatory field(s): newAmlsBody.isEmpty = " +
              s"${optNewAmlsBody.isEmpty}, newRegistrationNumber.isEmpty = ${optNewRegistrationNumber.isEmpty}"))
        }
      }
    }
  }

  private[amls] def buildSummaryListItems(
    isUkAgent: Boolean,
    journey: UpdateAmlsJourney,
    lang: Locale
  ): Seq[SummaryListData] = {
    for {
      body <- journey.newAmlsBody
      regNumber <- journey.newRegistrationNumber
    } yield {
      val items = Seq(
        SummaryListData(
          key = "amls.check-your-answers.supervisory-body",
          value = body,
          link = Some(amls.routes.AmlsNewSupervisoryBodyController.showPage(true))
        ),
        SummaryListData(
          key = "amls.check-your-answers.registration-number",
          value = regNumber,
          link = Some(amls.routes.EnterRegistrationNumberController.showPage(true))
        )
      )

      (isUkAgent, journey.newExpirationDate) match {
        case (false, _) => items
        case (true, Some(date)) =>
          val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", lang)
          items ++ Seq(SummaryListData(
            key = "amls.check-your-answers.renewal-date",
            value = date.format(formatter),
            link = Some(amls.routes.EnterRenewalDateController.showPage)
          ))
      }
    }
  }.getOrElse(throw new Exception("Expected AMLS journey data missing"))

}

/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.Logging
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentservicesaccount.forms.ContactDetailsSuspendForm
import uk.gov.hmrc.agentservicesaccount.models.{AuthProviderId, SuspendContentDetails}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.{contact_details, suspension_warning}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SuspendedJourneyController @Inject() (
                                             actions: Actions,
                                             suspensionWarningView: suspension_warning,
                                             contactDetailsView: contact_details,
                                             asc: AgentSubscriptionConnector,
                                             cacheService: SessionCacheService
                                           )(
                                             implicit val appConfig: AppConfig,
                                             val cc: MessagesControllerComponents,
                                             ec: ExecutionContext,
                                             messagesApi: MessagesApi
                                           ) extends AgentServicesBaseController with Logging {

  def showSuspendedWarning: Action[AnyContent] = actions.authAction { implicit request =>
    Ok(suspensionWarningView(request.agentInfo.isAdmin))
  }

  def showContactDetails: Action[AnyContent] = actions.authAction.async { implicit request =>

    contactForm(ContactDetailsSuspendForm.form)
  }

  def submitContactDetails: Action[AnyContent] = actions.authAction.async { implicit request =>
    ContactDetailsSuspendForm.form.bindFromRequest().fold(
      formWithErrors => {
        contactForm(formWithErrors)
      },
      formData => {
        cacheService.put(NAME, formData.name)
        cacheService.put(EMAIL, formData.email)
        cacheService.put(PHONE, formData.phone.getOrElse(""))
        maybeGetUtr().flatMap {
          case Some(utr) =>     cacheService.put(UTR, utr.value)
          case None =>
            cacheService.put(UTR, formData.utr.getOrElse(""))
        }

        Redirect("").toFuture
      }
    )
  }

  private def contactForm(form: Form[SuspendContentDetails])(implicit hc: HeaderCarrier, ec: ExecutionContext, rc: AuthRequestWithAgentInfo[AnyContent]): Future[Result] = {

    val maybeUtr = maybeGetUtr()
    maybeUtr.map {
      case Some(_) => Ok(contactDetailsView(form, true))
      case None => Ok(contactDetailsView(form))
    }
    Ok(contactDetailsView(form)).toFuture
  }

  def maybeGetUtr()(implicit hc: HeaderCarrier, ec: ExecutionContext, rc: AuthRequestWithAgentInfo[AnyContent]): Future[Option[Utr]] = {
    val maybeProviderId: Option[String] = rc.agentInfo.credentials.map(_.providerId)
    maybeProviderId match {
      case Some(pId) => asc.getJourneyById(AuthProviderId(pId)).map(_.map(_.businessDetails.utr))
      case None => Future.successful(None)
    }
  }
}

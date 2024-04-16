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
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject()(actions: Actions,
                                         sessionCache: SessionCacheService,
                                         draftDetailsService: DraftDetailsService,
                                         alfConnector: AddressLookupConnector,
                                         pcodRepository: PendingChangeOfDetailsRepository,
                                         agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                                         change_submitted: change_submitted,
                                         beforeYouStartPage: before_you_start_page
                                        )(implicit appConfig: AppConfig,
                                          cc: MessagesControllerComponents,
                                          ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with Logging {

  private def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  private def ifFeatureEnabledAndNoPendingChanges(action: => Future[Result])(implicit request: AuthRequestWithAgentInfo[_]): Future[Result] = {
    ifFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
      }
    }
  }

  val startAddressLookup: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val continueUrl: String = {
      val useAbsoluteUrls = appConfig.addressLookupBaseUrl.contains("localhost")
      val call = desiDetails.routes.ContactDetailsController.finishAddressLookup(None)
      if (useAbsoluteUrls) call.absoluteURL() else call.url
    }

    def languageLabels(implicit lang: Lang): JsObject = Json.obj(
      "countryPickerLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.country-picker.title"),
        "heading" -> messagesApi("update-contact-details.address.country-picker"),
        "countryLabel" -> ""
      ),
      "lookupPageLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.lookup.title"),
        "heading" -> messagesApi("update-contact-details.address.lookup")
      ),
      "selectPageLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.select.title")
      ),
      "editPageLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.edit.title")
      ),
      "confirmPageLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.confirm.title")
      )
    )

    val alfJourneyConfig = JourneyConfigV2(
      options = JourneyOptions(
        continueUrl = continueUrl
      ),
      version = 2,
      labels = Some(JourneyLabels(
        en = Some(languageLabels(Lang("en"))),
        cy = Some(languageLabels(Lang("cy")))
      ))
    )

    ifFeatureEnabledAndNoPendingChanges {
      alfConnector.init(alfJourneyConfig).map { addressLookupJourneyStartUrl =>
        Redirect(addressLookupJourneyStartUrl)
      }
    }
  }

  def finishAddressLookup(id: Option[String]): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      id match {
        case None => Future.successful(BadRequest)
        case Some(addressJourneyId) =>
          for {
            confirmedAddressResponse <- alfConnector.getAddress(addressJourneyId)
            newBusinessAddress = BusinessAddress(
              addressLine1 = confirmedAddressResponse.address.lines.get.head,
              addressLine2 = confirmedAddressResponse.address.lines.flatMap(_.drop(1).headOption),
              addressLine3 = confirmedAddressResponse.address.lines.flatMap(_.drop(2).headOption),
              addressLine4 = confirmedAddressResponse.address.lines.flatMap(_.drop(3).headOption),
              postalCode = confirmedAddressResponse.address.postcode,
              countryCode = confirmedAddressResponse.address.country.map(_.code).get
            )
            _ <- draftDetailsService.updateDraftDetails(
              desiDetails =>
                desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyAddress = Some(newBusinessAddress)))
            )
            nextPage <- getNextPage(sessionCache, "address")
          } yield {
            nextPage
          }
      }
    }
  }

  val showChangeSubmitted: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      Future.successful(Ok(change_submitted())) // don't show if there is nothing submitted
    }
  }

  def showBeforeYouStartPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableChangeContactDetails) {
      if (request.agentInfo.isAdmin) {
        agentClientAuthorisationConnector.getAgentRecord().map(agentRecord =>
          Ok(beforeYouStartPage(agentRecord.agencyDetails)))
      } else {
        Future.successful(Forbidden)
      }
    }
  }

}

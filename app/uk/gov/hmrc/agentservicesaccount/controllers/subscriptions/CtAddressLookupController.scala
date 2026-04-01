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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.i18n.Lang
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AddressLookupConnector
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.agentservicesaccount.services.AgentRecordService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtAddressLookupController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  alfConnector: AddressLookupConnector
)(implicit
  appConfig: AppConfig,
  agentRecordService: AgentRecordService,
  cc: MessagesControllerComponents,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  def startAddressLookup: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val continueUrl: String = {
      val useAbsoluteUrls = appConfig.addressLookupBaseUrl.contains("localhost")
      val call = subscriptions.routes.CtAddressLookupController.finishAddressLookup(None)
      if (useAbsoluteUrls)
        call.absoluteURL()
      else
        call.url
    }

//    TODO: 10906 Verify what these messages values should now be???
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

    alfConnector.init(alfJourneyConfig).map { addressLookupJourneyStartUrl =>
      Redirect(addressLookupJourneyStartUrl)
    }
  }

  def finishAddressLookup(id: Option[String]): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
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
//            _ <- draftDetailsService.updateDraftDetails(desiDetails =>
//              desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyAddress = Some(newBusinessAddress)))
//            )
//            journeyComplete <- isJourneyComplete()
//          TODO: 10906 Save ALF response to CtJourney
        } yield {
//            getNextPage(journeyComplete, "address")
//          TODO: 10906 Redirect using CtNextPageSelector
          Redirect(routes.AgentServicesController.root())
        }
    }
  }

}

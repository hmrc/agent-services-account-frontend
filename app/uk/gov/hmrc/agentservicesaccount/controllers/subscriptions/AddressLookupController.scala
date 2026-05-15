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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.addressLookupFinish
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AddressLookupController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  alfConnector: AddressLookupConnector
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  private def alfJourneyLanguageLabels(legacyRegime: LegacyRegime)(implicit lang: Lang): JsObject = {
    val legacyRegimePrefix = legacyRegime.msgPrefix
    val editPageLabels = Json.obj(
      "title" -> messagesApi(s"$legacyRegimePrefix.alf.edit.title"),
      "heading" -> messagesApi(s"$legacyRegimePrefix.alf.edit.heading"),
      "townLabel" -> messagesApi(s"$legacyRegimePrefix.alf.edit.townLabel")
    )
    Json.obj(
      "countryPickerLabels" -> Json.obj(
        "title" -> messagesApi(s"$legacyRegimePrefix.alf.country-picker.title"),
        "heading" -> messagesApi(s"$legacyRegimePrefix.alf.country-picker.heading"),
        "countryLabel" -> ""
      ),
      "lookupPageLabels" -> Json.obj(
        "title" -> messagesApi(s"$legacyRegimePrefix.alf.lookup.title"),
        "heading" -> messagesApi(s"$legacyRegimePrefix.alf.lookup.heading"),
        "postcodeLabel" -> messagesApi(s"$legacyRegimePrefix.alf.lookup.postcode.label")
      ),
      "selectPageLabels" -> Json.obj(
        "title" -> messagesApi(s"$legacyRegimePrefix.alf.select.title")
      ),
      "editPageLabels" -> editPageLabels,
      "international" -> Json.obj(
        "editPageLabels" -> editPageLabels
      ),
      "confirmPageLabels" -> Json.obj(
        "title" -> messagesApi(s"$legacyRegimePrefix.alf.confirm.title")
      )
    )
  }

  def startAddressLookup(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val continueUrl: String = {
      val useAbsoluteUrls = appConfig.addressLookupBaseUrl.contains("localhost")
      val call = subscriptions.routes.AddressLookupController.finishAddressLookup(None, legacyRegime)
      if (useAbsoluteUrls)
        call.absoluteURL()
      else
        call.url
    }

    val mandatoryFieldsConfig = MandatoryFieldsConfig(
      addressLine1 = Some(true),
      addressLine2 = Some(true),
      addressLine3 = Some(true),
      town = Some(false),
      postcode = None
    )

    val manualAddressEntryConfig = ManualAddressEntryConfig(
//      NOTE: 35 is min max length value that you can set with ALF. Our API requires 28 for lines1,2,3 and 18 for town
      line1MaxLength = Some(35),
      line2MaxLength = Some(35),
      line3MaxLength = Some(35),
      townMaxLength = Some(35),
      mandatoryFields = Some(mandatoryFieldsConfig),
      showOrganisationName = Some(false)
    )

    val alfJourneyConfig = JourneyConfigV2(
      options = JourneyOptions(
        continueUrl = continueUrl,
        manualAddressEntryConfig = Some(manualAddressEntryConfig),
        ukMode = Some(legacyRegime == PAYE)
      ),
      version = 2,
      labels = Some(JourneyLabels(
        en = Some(alfJourneyLanguageLabels(legacyRegime)(Lang("en"))),
        cy = Some(alfJourneyLanguageLabels(legacyRegime)(Lang("cy")))
      ))
    )

    alfConnector.init(alfJourneyConfig).map { addressLookupJourneyStartUrl =>
      Redirect(addressLookupJourneyStartUrl)
    }
  }

  def finishAddressLookup(
    id: Option[String],
    legacyRegime: LegacyRegime
  ): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

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
          updatedJourney = journey.copy(
            useCustomAddress = Some(true),
            addressAnswer = Some(newBusinessAddress)
          )
          _ <- sessionCacheService.put(subscriptionJourneyKey(legacyRegime), updatedJourney)
        } yield {
          Redirect(getNextPage(
            addressLookupFinish,
            Some(updatedJourney),
            legacyRegime
          ))
        }
    }
  }

}

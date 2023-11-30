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
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.updatecontactdetails._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject()
(
  actions:Actions,
  sessionCache: SessionCacheService,
  acaConnector: AgentClientAuthorisationConnector,
  alfConnector: AddressLookupConnector,
//views
  contact_details: contact_details,
  check_updated_details: check_updated_details,
  update_name: update_name,
  update_phone: update_phone,
  update_email: update_email,
  change_submitted: change_submitted,
)(implicit val appConfig: AppConfig,
                  val cc: MessagesControllerComponents,
                  ec: ExecutionContext,
                  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {


  def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  // utility function.
  def updateDraftDetails(f: AgencyDetails => AgencyDetails)(implicit request: Request[_]): Future[Unit] = for {
    mDraftDetailsInSession <- sessionCache.get[AgencyDetails](UPDATED_CONTACT_DETAILS)
    draftDetails <- mDraftDetailsInSession match {
      case Some(details) => Future.successful(details)
      // if there is no 'draft' new set of details in session, get a fresh copy of the current stored details
      case None => acaConnector.getAgencyDetails().map(_.getOrElse(throw new RuntimeException("Current agency details are unavailable")))
    }
    updatedDraftDetails = f(draftDetails)
    _ <- sessionCache.put[AgencyDetails](UPDATED_CONTACT_DETAILS, updatedDraftDetails)
  } yield ()



  val showCurrentContactDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      for {
        _ <- sessionCache.delete(UPDATED_CONTACT_DETAILS) // on displaying the 'current details' page, we delete any unsubmitted changes that may still be in session
        agencyDetails <- acaConnector.getAgencyDetails()
      } yield {
        Ok(contact_details(agencyDetails, request.agentInfo.isAdmin))
      }
    }
  }

  val showChangeBusinessName: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      Future.successful(Ok(update_name(UpdateDetailsForms.businessNameForm)))
    }
  }

  val submitChangeBusinessName: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      UpdateDetailsForms.businessNameForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_name(formWithErrors))),
          newAgencyName => {
            updateDraftDetails(_.copy(agencyName = Some(newAgencyName))).map(_ =>
              Redirect(routes.ContactDetailsController.showCheckNewDetails)
            )
          }
        )
    }
  }

  val showChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      Future.successful(Ok(update_email(UpdateDetailsForms.emailAddressForm)))
    }
  }

  val submitChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    // TODO - email verification
    ifFeatureEnabled {
      UpdateDetailsForms.emailAddressForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_email(formWithErrors))),
          newEmail => {
            updateDraftDetails(_.copy(agencyEmail = Some(newEmail))).map(_ =>
              Redirect(routes.ContactDetailsController.showCheckNewDetails)
            )
          }
        )
    }
  }

  val showChangeTelephoneNumber: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      Future.successful(Ok(update_phone(UpdateDetailsForms.telephoneNumberForm)))
    }
  }

  val submitChangeTelephoneNumber: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      UpdateDetailsForms.telephoneNumberForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_phone(formWithErrors))),
          newPhoneNumber => {
            updateDraftDetails(_.copy(agencyTelephone = Some(newPhoneNumber))).map(_ =>
              Redirect(routes.ContactDetailsController.showCheckNewDetails)
            )
          }
        )
    }
  }

  val startAddressLookup: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val continueUrl: String = {
      val useAbsoluteUrls = true
      val call = routes.ContactDetailsController.finishAddressLookup(None)
      if (useAbsoluteUrls) call.absoluteURL() else call.url
    }

    def languageLabels(implicit lang: Lang): JsObject = Json.obj(
      "countryPickerLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.enter-country"),
        "heading" -> messagesApi("update-contact-details.address.enter-country"),
        "countryLabel" -> ""
      ),
      "lookupPageLabels" -> Json.obj(
        "title" -> messagesApi("update-contact-details.address.enter-postcode"),
        "heading" -> messagesApi("update-contact-details.address.enter-postcode")
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

    ifFeatureEnabled {
      alfConnector.init(alfJourneyConfig).map { addressLookupJourneyStartUrl =>
        Redirect(addressLookupJourneyStartUrl)
      }
    }
  }

  def finishAddressLookup(id: Option[String]): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
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
            _ <- updateDraftDetails(_.copy(agencyAddress = Some(newBusinessAddress)))
          } yield {
            Redirect(routes.ContactDetailsController.showCheckNewDetails)
          }
      }
    }
  }


  val showCheckNewDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      sessionCache.get[AgencyDetails](UPDATED_CONTACT_DETAILS).map {
        case Some(updatedDetails) => Ok(check_updated_details(updatedDetails, request.agentInfo.isAdmin))
        case None => Redirect(routes.ContactDetailsController.showCurrentContactDetails)
      }
    }
  }

  val submitCheckNewDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      for {
        _ <- sessionCache.get[AgencyDetails](UPDATED_CONTACT_DETAILS)
        //
        // TODO actual connector call to submit the details goes here...
        //
        _ <- sessionCache.delete(UPDATED_CONTACT_DETAILS)
      } yield NotImplemented("Not implemented")
    }
  }

  val showChangeSubmitted: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      Future.successful(Ok(change_submitted())) // don't show if there is nothing submitted
    }
  }
}

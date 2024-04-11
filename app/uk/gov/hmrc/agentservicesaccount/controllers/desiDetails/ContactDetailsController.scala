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
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.CurrentAgencyDetails
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.models.emailverification.{Email, VerifyEmailRequest, VerifyEmailResponse}
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject()(actions: Actions,
                                         sessionCache: SessionCacheService,
                                         acaConnector: AgentClientAuthorisationConnector,
                                         alfConnector: AddressLookupConnector,
                                         evConnector: EmailVerificationConnector,
                                         pcodRepository: PendingChangeOfDetailsRepository,
                                         agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                                         //views
                                         view_contact_details: view_contact_details,
                                         update_name: update_name,
                                         update_phone: update_phone,
                                         update_email: update_email,
                                         change_submitted: change_submitted,
                                         email_locked: email_locked,
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
          Future.successful(Redirect(desiDetails.routes.ContactDetailsController.showCurrentContactDetails))
      }
    }
  }

  // utility function.
  private def updateDraftDetails(f: DesignatoryDetails => DesignatoryDetails)(implicit request: Request[_]): Future[Unit] = for {
    mDraftDetailsInSession <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
    draftDetails <- mDraftDetailsInSession match {
      case Some(details) => Future.successful(details)
      // if there is no 'draft' new set of details in session, get a fresh copy of the current stored details
      case None =>
        acaConnector.getAgentRecord()
        .map(agencyDetails=>
          DesignatoryDetails(
            agencyDetails = agencyDetails.agencyDetails.getOrElse(throw new RuntimeException("No agency details on agent record")),
            otherServices = OtherServices(
              saChanges = SaChanges(
                applyChanges = false,
                saAgentReference = None),
              ctChanges = CtChanges(
                applyChanges = false,
                ctAgentReference = None
              ))))
    }
    updatedDraftDetails = f(draftDetails)
    _ <- sessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, updatedDraftDetails)
  } yield ()

  val showCurrentContactDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      for {
        // on displaying the 'current details' page, we delete any unsubmitted changes that may still be in session
        _ <- sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)
        mPendingChange <- pcodRepository.find(request.agentInfo.arn)
        agencyDetails <- CurrentAgencyDetails.get(acaConnector)
      } yield Ok(view_contact_details(agencyDetails, mPendingChange, request.agentInfo.isAdmin))
    }
  }

  val showChangeBusinessName: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(update_name(UpdateDetailsForms.businessNameForm)))
    }
  }

  val submitChangeBusinessName: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.businessNameForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_name(formWithErrors))),
          newAgencyName => {
              updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyName = Some(newAgencyName)))).flatMap(_ =>
                getNextPage(sessionCache, "businessName")
            )
          }
        )
    }
  }

  val showChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(update_email(UpdateDetailsForms.emailAddressForm)))
    }
  }

  val submitChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.emailAddressForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_email(formWithErrors))),
          newEmail => {
            val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
            emailVerificationLogic(newEmail, credId)
          }
        )
    }
  }

  /* This is the callback endpoint (return url) from the email-verification service and not for use of our own frontend. */
  val finishEmailVerification: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      sessionCache.get(EMAIL_PENDING_VERIFICATION).flatMap {
        case Some(email) =>
          val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
          emailVerificationLogic(email, credId)
        case None => // this should not happen but if it does, return a graceful fallback response
          Future.successful(Redirect(desiDetails.routes.CheckYourAnswersController.showPage))
      }
    }
  }

  val showEmailLocked: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(email_locked()))
    }
  }

  val showChangeTelephoneNumber: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(update_phone(UpdateDetailsForms.telephoneNumberForm)))
    }
  }

  val submitChangeTelephoneNumber: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.telephoneNumberForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_phone(formWithErrors))),
          newPhoneNumber => {
            updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyTelephone = Some(newPhoneNumber)))).flatMap(_ =>
              getNextPage(sessionCache, "telephone")
            )
          }
        )
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
            _ <- updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyAddress = Some(newBusinessAddress))))
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

  private def emailVerificationLogic(newEmail: String, credId: String)(implicit request: Request[_]): Future[Result] = {
    val useAbsoluteUrls = appConfig.emailVerificationFrontendBaseUrl.contains("localhost")

    def makeUrl(call: Call): String = {
      if (useAbsoluteUrls) call.absoluteURL() else call.url
    }

    def emailCmp(l: String, r: String) = l.trim.equalsIgnoreCase(r.trim)

    for {
      mCurrentEmail <- acaConnector.getAgentRecord().map(_.agencyDetails.flatMap(_.agencyEmail))
      isUnchanged = mCurrentEmail.fold(false)(emailCmp(_, newEmail))
      previousVerifications <- evConnector.checkEmail(credId)
      previousVerification = previousVerifications.flatMap(_.emails.find(ce => emailCmp(ce.emailAddress, newEmail)))
      result <- previousVerification match {
        case _ if isUnchanged =>
          updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail)))).map(_ =>
            Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
          )
        case Some(pv) if pv.verified => // already verified
          for {
            _ <- updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail))))
            _ <- sessionCache.delete(EMAIL_PENDING_VERIFICATION)
          } yield Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
        case Some(pv) if pv.locked => // email locked due to too many attempts
          Future.successful(Redirect(desiDetails.routes.ContactDetailsController.showEmailLocked))
        case None => // email is not verified, start verification journey
          val lang = messagesApi.preferred(request).lang.code
          val verifyEmailRequest = VerifyEmailRequest(
            credId = credId,
            continueUrl = makeUrl(desiDetails.routes.ContactDetailsController.finishEmailVerification),
            origin = if (lang == "cy") "Gwasanaethau Asiant CThEM" else "HMRC Agent Services",
            deskproServiceName = None,
            accessibilityStatementUrl = "", // todo
            email = Some(Email(newEmail, makeUrl(desiDetails.routes.ContactDetailsController.showChangeEmailAddress))),
            lang = Some(lang),
            backUrl = Some(makeUrl(desiDetails.routes.CheckYourAnswersController.showPage)),
            pageTitle = None
          )
          for {
            _ <- sessionCache.put(EMAIL_PENDING_VERIFICATION, newEmail)
            mVerifyEmailResponse <- evConnector.verifyEmail(verifyEmailRequest)
          } yield mVerifyEmailResponse match {
            case Some(VerifyEmailResponse(redirectUri)) =>
              val redirect = if (useAbsoluteUrls) appConfig.emailVerificationFrontendBaseUrl + redirectUri else redirectUri
              Redirect(redirect)
            case None => InternalServerError
          }
      }
    } yield result
  }
}

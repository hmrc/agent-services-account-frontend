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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.UpscanInitiateConnector
import uk.gov.hmrc.agentservicesaccount.models.upscan.FileUploadReference
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanErrorCode
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanFailure
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanInProgress
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanInitiateRequest
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanSuccess
import uk.gov.hmrc.agentservicesaccount.repository.UpscanRepository
import uk.gov.hmrc.agentservicesaccount.services.ObjectStoreService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.amls_evidence_upload
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.amls_evidence_upload_progress
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.amls_evidence_upload_error
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.agentservicesaccount.controllers.internal.{routes => internalRoutes}

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EvidenceUploadController @Inject() (
  actions: Actions,
  objectStoreService: ObjectStoreService,
  upscanInitiateConnector: UpscanInitiateConnector,
  upscanRepository: UpscanRepository,
  val sessionCacheService: SessionCacheService,
  amlsEvidenceUploadPage: amls_evidence_upload,
  amlsEvidenceUploadProgressPage: amls_evidence_upload_progress,
  amlsEvidenceUploadErrorPage: amls_evidence_upload_error,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with AmlsJourneySupport
with I18nSupport {

  def showPage(failureReason: Option[String] = None): Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      withUpdateAmlsJourney {
        case amlsJourney if amlsJourney.newAmlsBody.isEmpty => Future.successful(Redirect(routes.AmlsNewSupervisoryBodyController.showPage()))
        case amlsJourney =>
          val initiateRequest = UpscanInitiateRequest(
            callbackUrl = s"${appConfig.asaFrontendBaseUrl}${internalRoutes.UpscanCallbackController.callback().url}",
            successRedirect = Some(s"${appConfig.asaFrontendExternalUrl}${routes.EvidenceUploadController.showUploadResult(None).url}"), // Key param will be populated by upscan itself
            errorRedirect = Some(s"${appConfig.asaFrontendExternalUrl}${routes.EvidenceUploadController.showUploadError().url}"),
            maximumFileSize = Some(appConfig.UpscanAmls.maxFileSize.toBytes)
          )
          for {
            initiateResponse <- upscanInitiateConnector.initiate(initiateRequest)
            _ <- upscanRepository.saveUpscanDetails(UpscanInProgress(initiateResponse.reference, Instant.now()))
          } yield Ok(amlsEvidenceUploadPage(
            upscanInitiateResponse = initiateResponse,
            supervisoryBodyName = amlsJourney.newAmlsBody.get,
            failureReason = failureReason
          ))
      }
  }

  def showUploadResult(key: Option[String]): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    withUpdateAmlsJourney { amlsJourney =>
      key match {
        case Some(reference) =>
          upscanRepository.findByReference(FileUploadReference(reference)).flatMap {
            case Some(details: UpscanSuccess) =>
              for {
                _ <- amlsJourney.newEvidenceObjectReference.fold(Future.unit)(reference => objectStoreService.deleteObject(reference))
                _ <- objectStoreService.transferFileToObjectStore(details)
                _ <- saveAmlsJourney(amlsJourney.copy(
                  newEvidenceObjectReference = Some(details.reference.value)
                ))
              } yield {
                Redirect(routes.CheckYourAnswersController.showPage)
              }
//              TODO: 11449: This is called in JS disabled flow to display UpscanFailure and UpscanInProgress details - UpscanFailure should now display in amlsEvidenceUploadPage
            case Some(details: UpscanInProgress) => Future.successful(Ok(amlsEvidenceUploadProgressPage(details)))
            case Some(details: UpscanFailure) => Future.successful(Redirect(routes.EvidenceUploadController.showPage(Some(details.failureReason)).url))
            case None => Future.successful(Redirect(routes.EvidenceUploadController.showPage().url))
          }
        case None => Future.successful(Redirect(routes.EvidenceUploadController.showPage().url))
      }
    }
  }

  /** Handles file upload errors from Upscan.
    *
    * This endpoint is called when a file transfer to Upscan service fails. It is not the endpoint for reporting file scanning failures, that happens in
    * showResult which reads the upload status from the application. Upscan will redirect to this endpoint and append error information as query parameters to
    * the redirect URL.
    */
  def showUploadError(
    errorCode: Option[String],
    errorMessage: Option[String],
    errorRequestId: Option[String],
    key: Option[String]
  ): Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>
    errorCode.flatMap(UpscanErrorCode.fromString) match {
      case Some(code) => Ok(amlsEvidenceUploadErrorPage(code))
      case None => Redirect(routes.EvidenceUploadController.showPage().url)
    }
  }

  // This endpoint is called via JavaScript in a poll loop to check the status of the file upload. The upload status is encoded in the HTTP status response:
  def checkUploadStatus(reference: String): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    implicit class ResultOps(result: Result) {
      def withCorsHeaders: Result = result.withHeaders(
        "Access-Control-Allow-Origin" -> appConfig.asaFrontendExternalUrl,
        "Access-Control-Allow-Credentials" -> "true",
        "Access-Control-Allow-Methods" -> "GET, OPTIONS"
      )
    }
    upscanRepository.findByReference(FileUploadReference(reference)).map {
//      TODO: 11449 Local workaround for checking JS enabled flow - need to modify in DB for JS disabled
      case _ => Conflict.withCorsHeaders
//      case Some(_: UpscanSuccess) => Accepted.withCorsHeaders
//      case Some(_: UpscanInProgress) => NoContent.withCorsHeaders
//      case Some(failure: UpscanFailure) if failure.failureReason == "QUARANTINE" => Conflict.withCorsHeaders
//      case Some(_: UpscanFailure) => BadRequest.withCorsHeaders // generic error as reason for file rejection is not known here, we do know it's not file type or file size as we are using JS validation - file could be corrupted for example
//      case None => NotFound.withCorsHeaders
    }
  }

}

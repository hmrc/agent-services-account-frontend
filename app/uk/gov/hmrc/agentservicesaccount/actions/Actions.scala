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

package uk.gov.hmrc.agentservicesaccount.actions

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.services.AgentRecordService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class Actions @Inject() (
  agentAssuranceConnector: AgentAssuranceConnector,
  agentRecordService: AgentRecordService,
  sessionCacheService: SessionCacheService,
  agentServicesAccountConnector: AgentServicesAccountConnector,
  authActions: AuthActions,
  actionBuilder: DefaultActionBuilder,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {

  private def filterSuspendedAgent(onlyForSuspended: Boolean): ActionFilter[AuthRequestWithAgentInfo] =
    new ActionFilter[AuthRequestWithAgentInfo] {
      def executionContext: ExecutionContext = ec

      def filter[A](request: AuthRequestWithAgentInfo[A]): Future[Option[Result]] = {
        implicit val req: Request[A] = request.request

        agentRecordService.getAgentRecord.map { agentRecord =>
          {
            (onlyForSuspended, agentRecord.suspensionDetails.exists(_.suspensionStatus)) match {
              case (true, true) | (false, false) => None
              case (true, false) => Some(Redirect(routes.AgentServicesController.showAgentServicesAccount()))
              case (false, true) => Some(Redirect(routes.SuspendedJourneyController.showSuspendedWarning()))
            }
          }
        }
      }
    }

  def authActionCheckSuspend: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen filterSuspendedAgent(false)

  def authActionOnlyForSuspended: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen filterSuspendedAgent(true)

  def ifFeatureEnabled(feature: Boolean)(action: => Future[Result]): Future[Result] = {
    if (feature)
      action
    else
      Future.successful(Forbidden)
  }

  case class AuthRequestWithAgentProfile[A](
    authRequestWithAgentInfo: AuthRequestWithAgentInfo[A],
    agentDetailsDesResponse: AgentDetailsDesResponse
  )
  extends WrappedRequest(authRequestWithAgentInfo.request)

  def withCurrentAmlsDetails(arn: Arn)(action: AmlsDetails => Future[Result])(implicit rh: RequestHeader): Future[Result] = {
    agentAssuranceConnector.getAMLSDetails(arn.value)
      .flatMap(amlsDetails => action(amlsDetails))
  }

  def withAgentRecord(onlyForSuspended: Boolean): ActionRefiner[AuthRequestWithAgentInfo, AuthRequestWithAgentProfile] =
    new ActionRefiner[AuthRequestWithAgentInfo, AuthRequestWithAgentProfile] {

      override protected def refine[A](request: AuthRequestWithAgentInfo[A]): Future[Either[Result, AuthRequestWithAgentProfile[A]]] = {
        implicit val req: RequestHeader = request.request

        agentRecordService.getAgentRecord.map(agentRecord => {
          (onlyForSuspended, agentRecord.suspensionDetails.exists(_.suspensionStatus)) match {
            case (true, true) | (false, false) => Right(AuthRequestWithAgentProfile(request, agentRecord))
            case (true, false) => Left(Redirect(routes.AgentServicesController.showAgentServicesAccount()))
            case (false, true) => Left(Redirect(routes.SuspendedJourneyController.showSuspendedWarning()))
          }
        })
      }
      override protected def executionContext: ExecutionContext = ec
    }

  def authActionForSuspendedAgentWithAgentRecord: ActionBuilder[AuthRequestWithAgentProfile, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen withAgentRecord(true)

  def authActionWithSuspensionCheckWithAgentRecord: ActionBuilder[AuthRequestWithAgentProfile, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen withAgentRecord(false)

  def authActionWithCtJourney: ActionBuilder[CtJourneyRequest, AnyContent] =
    actionBuilder andThen
      authActions.authActionRefiner andThen
      filterSuspendedAgent(false) andThen
      withCtJourney

  private def withCtJourney: ActionRefiner[AuthRequestWithAgentInfo, CtJourneyRequest] =
    new ActionRefiner[AuthRequestWithAgentInfo, CtJourneyRequest] {
      override protected def executionContext: ExecutionContext = ec
      override protected def refine[A](
        request: AuthRequestWithAgentInfo[A]
      ): Future[Either[Result, CtJourneyRequest[A]]] = {
        implicit val req: Request[A] = request.request
        def buildRequest(journey: CtJourney): CtJourneyRequest[A] =
          new CtJourneyRequest(
            ctSubscriptionJourney = journey,
            agentInfo = request.agentInfo,
            request = request.request
          )
        def ctJourney(asaDetails: AgencyDetails) = CtJourney(
          asaDetails = asaDetails,
          businessNameFlag = false,
          businessNameAnswer = None,
          phoneNumberFlag = false,
          phoneNumberAnswer = None,
          emailFlag = false,
          emailAnswer = None,
          addressFlag = false,
          addressAnswer = None
        )
        implicit val ctJourneyFormat: OFormat[CtJourney] = Json.format[CtJourney]
        val resultF: Future[CtJourneyRequest[A]] =
          if (appConfig.enableLegacySubscriptionLink) {
            sessionCacheService.get[CtJourney](ctJourneyKey).flatMap {
              case Some(journey) => Future.successful(buildRequest(journey))
              case None =>
                agentServicesAccountConnector.getAgentRecord.flatMap { response =>
                  val journey = ctJourney(response.agencyDetails.getOrElse(AgencyDetails(
                    None,
                    None,
                    None,
                    None
                  )))
                  sessionCacheService.put(ctJourneyKey, journey).map { _ => buildRequest(journey) }
                }
            }
          }
          else {
            // TODO: Feature disabled → initialise empty journey (no cache interaction); Should it return forbidden?
            val emptyJourney = ctJourney(AgencyDetails(
              None,
              None,
              None,
              None
            ))
            Future.successful(buildRequest(emptyJourney))
          }
        resultF.map(Right(_))
      }
    }

}

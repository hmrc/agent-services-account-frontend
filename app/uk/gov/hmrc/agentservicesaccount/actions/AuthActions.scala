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

import play.api.mvc.Results._
import play.api.mvc._
import play.api.Environment
import play.api.Logging
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionInfo
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
class AuthRequestWithAgentInfo[A](
  val agentInfo: AgentInfo,
  val request: Request[A]
)
extends WrappedRequest[A](request)

case class AgentInfo(
  arn: Arn,
  enrolments: Enrolments,
  credentialRole: Option[CredentialRole],
  email: Option[String] = None,
  name: Option[Name] = None,
  credentials: Option[Credentials] = None
) {

  val isAdmin: Boolean =
    credentialRole match {
      case Some(Assistant) => false
      case Some(_) => true
      case _ => false
    }

  private val hasPayeSubscription: Boolean = enrolments.getEnrolment("IR-PAYE-AGENT").exists(_.isActivated)
  private val hasCtSubscription: Boolean = enrolments.getEnrolment("IR-CT-AGENT").exists(_.isActivated)
  private val hasSaSubscription: Boolean = enrolments.getEnrolment("IR-SA-AGENT").exists(_.isActivated)

  def existingSubscriptionInfo: Seq[SubscriptionInfo] =
    Seq(
      if (hasPayeSubscription)
        Some(SubscriptionInfo(LegacyRegime.PAYE, SubscriptionStatus.Subscribed))
      else
        None,
      if (hasCtSubscription)
        Some(SubscriptionInfo(LegacyRegime.CT, SubscriptionStatus.Subscribed))
      else
        None,
      if (hasSaSubscription)
        Some(SubscriptionInfo(LegacyRegime.SA, SubscriptionStatus.Subscribed))
      else
        None
    ).flatten

  def missingSubscriptions: Seq[LegacyRegime] =
    Seq(
      if (!hasPayeSubscription)
        Some(LegacyRegime.PAYE)
      else
        None,
      if (!hasCtSubscription)
        Some(LegacyRegime.CT)
      else
        None,
      if (!hasSaSubscription)
        Some(LegacyRegime.SA)
      else
        None
    ).flatten

}

@Singleton
class AuthActions @Inject() (
  appConfig: AppConfig,
  override val authConnector: AuthConnector,
  val env: Environment
)(implicit ec: ExecutionContext)
extends AuthorisedFunctions
with Logging {

  def authActionRefiner: ActionRefiner[Request, AuthRequestWithAgentInfo] =
    new ActionRefiner[Request, AuthRequestWithAgentInfo] {

      override protected def refine[A](request: Request[A]): Future[Either[Result, AuthRequestWithAgentInfo[A]]] = {
        implicit val r: Request[A] = request

        authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
          .retrieve(allEnrolments and credentials and email and name and credentialRole) {
            case enrols ~ creds ~ email ~ name ~ credRole =>
              getArn(enrols) match {
                case Some(arn) =>
                  Future.successful(Right(new AuthRequestWithAgentInfo(
                    AgentInfo(
                      arn = arn,
                      enrolments = enrols,
                      credentialRole = credRole,
                      email = email,
                      name = name,
                      credentials = creds
                    ),
                    r
                  )))
                case None =>
                  logger.warn("No HMRC-AS-AGENT enrolment found -- redirecting to /agent-subscription/start.")
                  Future successful Left(Redirect(appConfig.agentSubscriptionFrontendUrl))
              }
          }.recover(handleFailureRefiner)

      }

      override protected def executionContext: ExecutionContext = ec
    }

  private def handleFailureRefiner[A](implicit request: RequestHeader): PartialFunction[Throwable, Either[Result, AuthRequestWithAgentInfo[A]]] = {
    case _: NoActiveSession => Left(Redirect(s"${appConfig.signInUrl}?continue_url=${appConfig.continueUrl}${request.uri}&origin=${appConfig.appName}"))

    case _: UnsupportedAuthProvider =>
      logger.warn(s"user logged in with unsupported auth provider")
      Left(Forbidden)

    case _: UnsupportedAffinityGroup =>
      logger.warn(s"user logged in with unsupported affinity group")
      Left(Forbidden)
  }

  private def getArn(enrolments: Enrolments): Option[Arn] = {
    for {
      agentEnrol <- enrolments.getEnrolment("HMRC-AS-AGENT")
      enrolId <- agentEnrol.getIdentifier("AgentReferenceNumber")
    } yield {
      Arn(enrolId.value)
    }
  }

}

object AuthActions {
  type AgentAuthAction = Request[AnyContent] => AgentInfo => Future[Result]
}

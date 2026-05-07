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

package uk.gov.hmrc.agentservicesaccount.config

import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() ()
extends InternalAuthTokenInitialiser {
  override val initialised: Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
extends InternalAuthTokenInitialiser
with Logging {

  override val initialised: Future[Done] =
    for {
      _ <- ensureAuthToken()
    } yield Done

  Await.result(initialised, 30.seconds)

  private def ensureAuthToken(): Future[Done] = authTokenIsValid.flatMap { isValid =>
    if (isValid) {
      logger.info("Auth token is already valid")
      Future.successful(Done)
    }
    else {
      createClientAuthToken()
    }
  }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("Initialising auth token")
    httpClient
      .post(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        // language=JSON
        Json.parse(
          s"""
             |{
             | "token": "${appConfig.internalAuthToken}",
             | "principal": "${appConfig.appName}",
             | "permissions": [
             |   {
             |    "resourceType": "object-store",
             |    "resourceLocation": "*",
             |    "actions": ["*"]
             |   }
             | ]
             |}
             |""".stripMargin
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == CREATED) {
          logger.info("Auth token initialised")
          Future.successful(Done)
        }
        else {
          logger.warn("Unable to initialise internal-auth token")
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }

  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient
      .get(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> appConfig.internalAuthToken)
      .execute
      .map(_.status == 200)
  }

}

/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import play.api.libs.json.Json
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

case class AgentAdmin(credId: String)
object AgentAdmin {
  implicit val format = Json.format[AgentAdmin]
}

@Singleton
class UserDelegationConnector @Inject()(@Named("user-delegation-baseUrl") baseUrl: URL, httpGet: HttpGet) {
  def isAdmin(suppliedCredId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val url = new URL(baseUrl, s"/user-delegation/admins")

    httpGet.GET[HttpResponse](url.toString).map { response =>
      response.status match {
        case 200 => (response.json \ "currentUser").toOption match {
          case Some(currentUser) => currentUser.as[AgentAdmin].credId == suppliedCredId
          case None => false
        }
        case _ => false
      }
    }.recoverWith {
      case _: Upstream5xxResponse | _: HttpException => Future.successful(false)
    }
  }
}

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

package uk.gov.hmrc.agentservicesaccount
import uk.gov.hmrc.mongo.cache.DataKey
import scala.concurrent.Future

package object controllers {
  implicit class ToFuture[T](t: T) {
    def toFuture: Future[T] = Future successful t
  }

  val AGENT_SIZE: DataKey[String] = DataKey("numberOfClients")
  val NAME: DataKey[String] = DataKey("name")
  val EMAIL: DataKey[String] = DataKey("email")
  val PHONE: DataKey[String] = DataKey("phone")
  val UTR: DataKey[String] = DataKey("utr")

  //todo put in my ones
  val sessionKeys =
    List(
      AGENT_SIZE,
      NAME,
      EMAIL,
      PHONE,
      UTR
    )

}

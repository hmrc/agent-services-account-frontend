/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, UserDetails}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentUserClientDetailsStubs.stubGetTeamMembersForArn
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class AgentUserClientDetailsConnectorSpec extends BaseISpec {

  private lazy val connector = app.injector.instanceOf[AgentUserClientDetailsConnector]
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val arn: Arn = Arn("TARN0000001")

  "getTeamMembers" should {
    "return the team members" in {
      val teamMembers = List(
        UserDetails(credentialRole = Some("User"), name = Some("Robert Builder"), email = Some("bob@builder.com")),
        UserDetails(credentialRole = Some("User"), name = Some("Steve Smith"), email = Some("steve@builder.com")),
        //Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant")),
      )
      stubGetTeamMembersForArn(arn, teamMembers)
      await(connector.getTeamMembers(arn)) shouldBe Some(teamMembers)
    }

  }
}

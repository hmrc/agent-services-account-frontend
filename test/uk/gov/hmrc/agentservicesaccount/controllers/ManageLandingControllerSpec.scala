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

package uk.gov.hmrc.agentservicesaccount.controllers


import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.optin.{OptedInReady, OptedOutSingleUser}
import uk.gov.hmrc.agentservicesaccount.models.AccessGroupSummaries
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs.{givenAccessGroupsForArn, givenArnAllowedOk, givenOptinStatusSuccessReturnsForArn, givenSyncEacdSuccess}
import uk.gov.hmrc.agentservicesaccount.support.Css.{H1, paragraphs}
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._

class ManageLandingControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  val controller: ManageLandingController = app.injector.instanceOf[ManageLandingController]

  "showAccessGroupSummaryForASA" should  {

    val ASAAccountTitle = "Manage access in the agent services account - Agent services account - GOV.UK"


    "return Status: Forbidden" in {
      givenAuthorisedAsAgentWith(arn, isAdmin = false)
      givenAgentRecordFound(agentRecord)
      val response = await(controller.showAccessGroupSummaryForASA(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ"))) //URL response created to mock webpage

      status(response) shouldBe 403
    }

    "return page correct content when OptOut" in {
      // Given: auth agent with no opt in status
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutSingleUser)
      givenAgentRecordFound(agentRecord)
      // When:
      val response = await(controller.showAccessGroupSummaryForASA()(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ")))
      status(response) shouldBe 200
      // Then: page shown is for when access groups are turned off
      val html = Jsoup.parse(contentAsString(response))
      val p = html.select(paragraphs)

      p.get(0).text shouldBe "For this tax service, use access groups to control which team members can manage this client. This is done in your agent services account."
      p.get(1).text shouldBe "This tax service is managed through your agent services account. Access permissions for the agent services account work differently from other HMRC online services."
      p.get(2).text shouldBe "By default, all your team members have access to all your clients. You can restrict access to a client’s taxes using access groups."
      p.get(3).text shouldBe "To find out more, select ‘Turn on access groups’ on the ‘Manage account’ page. You’ll be shown more information. You can then choose to turn access groups on or leave them off."

    }

    "return Status: OK & page with correct content whilst Optin" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentRecordFound(agentRecord)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady) // access groups turned on
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty)) // no access groups yet

      val response = await(controller.showAccessGroupSummaryForASA()(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ")))

      status(response) shouldBe 200
      // Then: page shown is for when access groups are turned on
      val html = Jsoup.parse(contentAsString(response))

      val p = html.select(Css.paragraphs)

      html.title() shouldBe ASAAccountTitle
      html.select(H1).get(0).text shouldBe "Manage access in the agent services account"
      p.get(0).text
        .shouldBe("For this tax service, use access groups to control which team members can manage this client. This is done in your agent services account.")
      p.get(1).text
        .shouldBe("This tax service is managed through your agent services account. Access permissions for the agent services account work differently from other HMRC online services.")
      p.get(2).text
        .shouldBe("Your organisation can restrict access to a client’s taxes using access groups. If a client is not in any access groups, any team member can manage their tax. If a client is in access groups, only team members in the same groups can manage their tax.")
      p.get(3).text
        .shouldBe("Your organisation has turned access groups on. You can create new access groups or view existing groups from the ‘Manage account’ screen.")
    }

  }

}

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

package uk.gov.hmrc.agentservicesaccount.controllers

import org.apache.commons.lang3.RandomUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Session
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentUserClientDetailsStubs._
import uk.gov.hmrc.agentservicesaccount.support.Css._
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys


class BetaInviteControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: AgentServicesController = app.injector.instanceOf[AgentServicesController]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  private def fakeRequest(method: String = "GET", uri: String = "/") = FakeRequest(method, uri).withSession(SessionKeys.authToken -> "Bearer XYZ")

  "private beta testing" should {

    "return status: OK" when {
      "IRV allowlist is enabled and the ARN is allowed (suspension disabled)" in {
        givenArnIsAllowlistedForIrv(Arn(arn))
        val controller =
          appBuilder(Map("features.enable-agent-suspension" -> false, "features.enable-irv-allowlist" -> true))
            .build()
            .injector.instanceOf[AgentServicesController]
        givenAuthorisedAsAgentWith(arn)

        val result = controller.showAgentServicesAccount(fakeRequest())
        status(result) shouldBe OK
      }

      "IRV allowlist is disabled and no suspension status" in {
        val controller =
          appBuilder(Map("features.enable-irv-allowlist" -> false, "features.enable-agent-suspension" -> true))
            .build()
            .injector.instanceOf[AgentServicesController]
        givenSuspensionStatusNotFound
        givenAuthorisedAsAgentWith(arn)

        val result = controller.showAgentServicesAccount(fakeRequest())
        status(result) shouldBe OK
      }

      "suspension details are in the session and agent is suspended for VATC (with IRV enabled)" in {
        givenArnIsAllowlistedForIrv(Arn(arn))
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
        givenAuthorisedAsAgentWith(arn)
        val response = controller.showAgentServicesAccount()(fakeRequest("GET", "/home"))

        status(response) shouldBe OK
      }

    }

    "return body containing the correct content" when {

      def expectedBetaInviteContent(html: Document): Assertion = {
        html.select(paragraphs).get(0).text shouldBe "Help improve our new feature"
        html.select(paragraphs).get(1).text shouldBe "Try out access groups and tell us what you think."
        html.select(link).get(0).text shouldBe "Tell me more"
        html.select(link).get(0).attr("href") shouldBe "/agent-services-account/private-beta-invite"
        html.select(SUBMIT_BUTTON).get(0).text shouldBe "No thanks"
      }

      "an authorised agent with suspension disabled and IRV enabled and allowed" in {
        givenArnIsAllowlistedForIrv(Arn(arn))
        val controllerWithSuspensionDisabled =
          appBuilder(Map("features.enable-agent-suspension" -> false))
            .build()
            .injector.instanceOf[AgentServicesController]
        givenAuthorisedAsAgentWith(arn)
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

        val response = await(controllerWithSuspensionDisabled.showAgentServicesAccount()(fakeRequest("GET", "/home")))
        val html = Jsoup.parse(contentAsString(response))

        val p = html.select(paragraphs)
        val a = html.select(link)

        expectedTitle(html, "Welcome to your agent services account - Agent services account - GOV.UK")

        // beta banner present - only when gran perms enabled?
        assertElementContainsText(html, cssSelector = "div.govuk-phase-banner a", expectedText = "feedback")
        assertAttributeValueForElement(
          element = html.select("div.govuk-phase-banner a").get(0),
          attributeValue = "http://localhost:9250/contact/beta-feedback?service=AOSS"
        )

        expectedBetaInviteContent(html)

        // accordion includes Income Record Viewer section
        expectedH2(html, "Tax services you can manage in this account", 1)

        expectedH2(html, "Help and guidance", 2)
        p.get(20).text shouldBe "Find out how to use your agent services account and how clients can authorise you to manage their taxes"
        a.get(28).attr("href") shouldBe "/agent-services-account/help-and-guidance"
      }
    }


  }
}

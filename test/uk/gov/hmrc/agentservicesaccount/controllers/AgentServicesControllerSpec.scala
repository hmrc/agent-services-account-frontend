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
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agents.accessgroups.optin._
import uk.gov.hmrc.agents.accessgroups.{GroupSummary, UserDetails}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AccessGroupSummaries, AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentUserClientDetailsStubs._
import uk.gov.hmrc.agentservicesaccount.support.Css._
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys

import java.util.UUID


class AgentServicesControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: AgentServicesController = app.injector.instanceOf[AgentServicesController]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  val groupId1: UUID = UUID.randomUUID()

  val customSummary: GroupSummary = GroupSummary(groupId1, "Potatoes", Some(1), 1)
  val taxSummary: GroupSummary = GroupSummary(UUID.randomUUID(), "TRust me", None, 1, Some("HMRC-TERS"))

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  private def fakeRequest(method: String = "GET", uri: String = "/") = FakeRequest(method, uri).withSession(SessionKeys.authToken -> "Bearer XYZ")

  "root" should {

    "redirect to agent service account when the user is not suspended" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when user is suspended" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
    }

    "redirect to suspended warning when user is suspended for AGSV" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("authToken" -> "Bearer XYZ", "suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "redirect to suspended warning when user is suspended for ALL" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ALL"))))

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("authToken" -> "Bearer XYZ", "suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "throw an exception when suspension status returns NOT_FOUND for user" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentRecordNotFound


      intercept[SuspensionDetailsNotFound] {
        await(controller.root()(fakeRequest()))
      }
    }
  }

  "home" should {

    "return status: OK" when {

      "No suspension status" in {
        givenSuspensionStatusNotFound
        givenAuthorisedAsAgentWith(arn)
        givenHidePrivateBetaInviteNotFound()

        val result = controller.showAgentServicesAccount(fakeRequest())
        status(result) shouldBe OK
      }

      "Agent is suspended for VATC (suspension details are in the session)" in {
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
        givenAuthorisedAsAgentWith(arn)
        givenHidePrivateBetaInviteNotFound()

        val response = controller.showAgentServicesAccount()(fakeRequest("GET", "/home"))
        status(response) shouldBe OK
      }

    }

    "return body containing the correct content" when {

      def expectedHomeBannerContent(html: Document): Assertion = {
        expectedH1(html, "Welcome to your agent services account")
        assertPageContainsText(html, "Account number: TARN 000 0001")
      }

      def expectedBetaInviteContent(html: Document): Assertion = {
        html.select(paragraphs).get(0).text shouldBe "Help improve our new feature"
        html.select(paragraphs).get(1).text shouldBe "Try out access groups and tell us what you think."
        html.select(link).get(0).text shouldBe "Tell me more"
        html.select(link).get(0).attr("href") shouldBe "/agent-services-account/private-beta-testing"
        html.select(SUBMIT_BUTTON).get(0).text shouldBe "No thanks"
      }

      def expectedClientAuthContent(html: Document, betaInviteContent: Boolean = true): Assertion = {
        expectedH2(html, "Client authorisations")
        html.select(paragraphs).get(if (betaInviteContent) 2 else 0).text shouldBe "You must ask your client to authorise you through your agent services account before you can access any services. Copy across an old authorisation or create a new one."
        expectTextForElement(html.select(LI).get(0), "Ask a client to authorise you")
        assertAttributeValueForElement(html.select(link).get(if (betaInviteContent) 1 else 0), attributeValue = "http://localhost:9448/invitations/agents")
        expectTextForElement(html.select(LI).get(1), "Manage your authorisation requests from the last 30 days")
        assertAttributeValueForElement(html.select(link).get(if (betaInviteContent) 2 else 1), attributeValue = "http://localhost:9448/invitations/track")
        expectTextForElement(html.select(LI).get(2), "Copy across more VAT and Self Assessment client authorisations")
        assertAttributeValueForElement(html.select(link).get(if (betaInviteContent) 3 else 2), attributeValue = "http://localhost:9438/agent-mapping/start")
        expectTextForElement(html.select(LI).get(3), "Cancel a client’s authorisation")
        assertAttributeValueForElement(html.select(link).get(if (betaInviteContent) 4 else 3), attributeValue = "http://localhost:9448/invitations/agents/cancel-authorisation/client-type")
      }

      "an authorised agent with no suspension" in {
        givenAuthorisedAsAgentWith(arn)
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))
        givenHidePrivateBetaInviteNotFound()

        val response = await(controller.showAgentServicesAccount()(fakeRequest("GET", "/home")))
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

        expectedHomeBannerContent(html)
        expectedBetaInviteContent(html)
        expectedClientAuthContent(html)

        // accordion includes Income Record Viewer section
        expectedH2(html, "Tax services you can manage in this account", 1)

        expectedH3(html, "Making Tax Digital for Income Tax")
        expectedH4(html, "Before you start")
        p.get(3).text shouldBe "You must first get an authorisation from your client. You can do this by copying across your authorisations or requesting an authorisation."
        a.get(5).attr("href") shouldBe "http://localhost:9438/agent-mapping/start"
        a.get(6).attr("href") shouldBe "http://localhost:9448/invitations/agents/client-type"
        p.get(4).text shouldBe "If you copy your client across, you will need to sign them up to Making Tax Digital for Income Tax (opens in a new tab)"
        a.get(7).attr("href") shouldBe "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-income-tax"
        expectedH4(html, "Manage your client’s Income Tax details", 1)
        p.get(5).text shouldBe "View your client’s Income Tax"
        a.get(8).attr("href") shouldBe "http://localhost:9081/report-quarterly/income-and-expenses/view/agents"
        p.get(6).text shouldBe "Help clients check whether they are eligible (opens in a new tab)"
        a.get(9).attr("href") shouldBe "https://www.gov.uk/guidance/follow-the-rules-for-making-tax-digital-for-income-tax#who-can-follow-the-rules"

        expectedH3(html, "VAT", 1)
        expectedH4(html, "Before you start", 2)
        p.get(7).text shouldBe "You must first get an authorisation from your client."
        p.get(7).select("a").text shouldBe "You must first get an authorisation from your client."
        p.get(7).select("a").attr("href") shouldBe "http://localhost:9448/invitations/agents/client-type"

        //        a.get(10).attr("href") shouldBe "https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat"
        expectedH4(html, "Manage your client’s VAT", 3)
        a.get(11).text shouldBe "Register your client for VAT (opens in a new tab)"
        a.get(11).attr("href") shouldBe "https://www.tax.service.gov.uk/register-for-vat"
        a.get(12).text shouldBe "Manage, submit and view your client’s VAT details (opens in a new tab)"
        a.get(12).attr("href") shouldBe "http://localhost:9149/vat-through-software/representative/client-vat-number"

        expectedH3(html, "View a client’s Income record", 2)
        p.get(8).text shouldBe "Access a client’s Income record to help you complete their Self Assessment tax return."
        p.get(9).text shouldBe "View a client’s Income record"
        a.get(13).attr("href") shouldBe "http://localhost:9996/tax-history/select-client"

        expectedH3(html, "Trusts and estates", 3)
        expectedH4(html, "Before you start", 4)
        p.get(10).text shouldBe "Before you ask your client to authorise you, you or your client must have registered the trust (opens in a new tab) or estate (opens in a new tab)."
        a.get(14).attr("href") shouldBe "http://localhost:9448/invitations/agents/client-type"
        a.get(15).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-trust"
        a.get(16).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-estate"
        p.get(11).text shouldBe "Your client will need to claim the trust or estate."
        a.get(17).attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service#how-to-use-the-online-service"
        expectedH4(html, "Manage your client’s trust", 5)
        p.get(12).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register ."
        a.get(18).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register"
        a.get(18).attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service"

        expectedH3(html, "Capital Gains Tax on UK property", 4)
        expectedH4(html, "Before you start", 6)
        p.get(13).text shouldBe "Your client must first set up a Capital Gains Tax on UK property account (opens in a new tab)"
        a.get(19).attr("href")
          .shouldBe("https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#before-you-start")
        p.get(14).text shouldBe "They must then authorise you to act on their behalf (opens in a new tab)"
        a.get(20).attr("href") shouldBe "https://www.gov" +
          ".uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#get-authorisation"
        expectedH4(html, "Manage a client’s Capital Gains Tax on UK property", 7)
        a.get(21).text shouldBe "Report your client’s Capital Gains Tax on UK property and view payments and penalties"
        a.get(21).attr("href") shouldBe "https://www.tax.service.gov.uk/capital-gains-tax-uk-property/start"

        expectedH3(html, "Plastic Packaging Tax", 5)
        expectedH4(html, "Before you start", 8)
        p.get(16).text shouldBe "Your client must first register for Plastic Packaging Tax (opens in a new tab)"
        a.get(22).attr("href") shouldBe "https://www.gov.uk/guidance/register-for-plastic-packaging-tax"
        p.get(17).text shouldBe "They must then authorise you to act on their behalf"
        a.get(23).attr("href") shouldBe "http://localhost:9448/invitations/agents"
        expectedH4(html, "Manage your client’s Plastic Packaging Tax", 9)
        p.get(18).text shouldBe "Report your client’s Plastic Packaging Tax and view payments, returns and penalties"
        a.get(24).attr("href") shouldBe "https://www.tax.service.gov.uk/plastic-packaging-tax/account"

        expectedH3(html, "Other tax services", 6)
        html.select(".govuk-warning-text").text shouldBe "! The agent services account is the home for HMRC tax services launched from 2019. For any tax services not listed here, sign out of this account and log in to your HMRC online services for agents account (opens in new tab)."
        a.get(25).attr("href") shouldBe "https://www.gov.uk/government/collections/hmrc-online-services-for-agents#hmrc-online-services-for-agents-account"
        // end of accordion

        expectedH2(html, "Help and guidance", 2)
        p.get(19).text shouldBe "Find out how to use your agent services account and how clients can authorise you to manage their taxes"
        a.get(26).attr("href") shouldBe "/agent-services-account/help-and-guidance"
      }

      "agent is suspended for VATC and suspension details are in the session" in {
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
        givenAuthorisedAsAgentWith(arn)

        givenHidePrivateBetaInvite()

        val response = await(controller.showAgentServicesAccount()(fakeRequest("GET", "/home")))
        val html = Jsoup.parse(contentAsString(response))
        val p = html.select(paragraphs)
        val a = html.select(link)

        expectedHomeBannerContent(html)
        expectedClientAuthContent(html, betaInviteContent = false)

        // accordion with suspension content includes Income Record Viewer section
        expectedH2(html, "Tax services you can manage in this account", 1)

        expectedH3(html, "Making Tax Digital for Income Tax")
        expectedH4(html, "Before you start")
        expectTextForElement(p.get(1),
          "You must first get an authorisation from your client. You can do this by copying across your authorisations or requesting an authorisation.")
        assertAttributeValueForElement(a.get(4), attributeValue = "http://localhost:9438/agent-mapping/start")
        assertAttributeValueForElement(a.get(5), attributeValue = "http://localhost:9448/invitations/agents/client-type")
        expectTextForElement(p.get(2),
          "If you copy your client across, you will need to sign them up to Making Tax Digital for Income Tax (opens in a new tab)")
        assertAttributeValueForElement(a.get(6), attributeValue = "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-income-tax")
        expectedH4(html, "Manage your client’s Income Tax details", 1)
        expectTextForElement(p.get(3), "View your client’s Income Tax")
        assertAttributeValueForElement(a.get(7), attributeValue = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents")
        expectTextForElement(p.get(4), "Help clients check whether they are eligible (opens in a new tab)")
        assertAttributeValueForElement(a.get(8), attributeValue = "https://www.gov.uk/guidance/follow-the-rules-for-making-tax-digital-for-income-tax#who-can-follow-the-rules")

        // VAT suspended section
        expectedH3(html, "VAT", 1)
        expectedH4(html, "We have temporarily limited your use of this service", 2)
        p.get(5).text shouldBe "We did this because we have suspended your agent code. We sent you a letter to confirm this."
        p.get(6).text shouldBe "This means you will not be able to use this service."

        expectedH3(html, "View a client’s Income record", 2)
        p.get(7).text shouldBe "Access a client’s Income record to help you complete their Self Assessment tax return."
        p.get(8).text shouldBe "View a client’s Income record"
        a.get(9).attr("href") shouldBe "http://localhost:9996/tax-history/select-client"

        expectedH3(html, "Trusts and estates", 3)
        expectedH4(html, "Before you start", 3)
        p.get(9).text shouldBe "Before you ask your client to authorise you, you or your client must have registered the trust (opens in a new tab) or estate (opens in a new tab)."
        a.get(10).attr("href") shouldBe "http://localhost:9448/invitations/agents/client-type"
        a.get(11).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-trust"
        a.get(12).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-estate"
        p.get(10).text shouldBe "Your client will need to claim the trust or estate."
        a.get(13).attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service#how-to-use-the-online-service"
        expectedH4(html, "Manage your client’s trust", 4)
        p.get(11).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register ."
        a.get(14).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register"
        a.get(14).attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service"

        expectedH3(html, "Capital Gains Tax on UK property", 4)
        expectedH4(html, "Before you start", 5)
        p.get(12).text shouldBe "Your client must first set up a Capital Gains Tax on UK property account (opens in a new tab)"
        a.get(15).attr("href") shouldBe "https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#before-you-start"
        p.get(13).text shouldBe "They must then authorise you to act on their behalf (opens in a new tab)"
        a.get(16).attr("href") shouldBe "https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#get-authorisation"
        expectedH4(html, "Manage a client’s Capital Gains Tax on UK property", 6)
        a.get(17).text shouldBe "Report your client’s Capital Gains Tax on UK property and view payments and penalties"
        a.get(17).attr("href") shouldBe "https://www.tax.service.gov.uk/capital-gains-tax-uk-property/start"

        expectedH3(html, "Plastic Packaging Tax", 5)
        expectedH4(html, "Before you start", 7)
        p.get(15).text shouldBe "Your client must first register for Plastic Packaging Tax (opens in a new tab)"
        a.get(18).attr("href") shouldBe "https://www.gov.uk/guidance/register-for-plastic-packaging-tax"
        p.get(16).text shouldBe "They must then authorise you to act on their behalf"
        a.get(19).attr("href") shouldBe "http://localhost:9448/invitations/agents"
        expectedH4(html, "Manage your client’s Plastic Packaging Tax", 8)
        p.get(17).text shouldBe "Report your client’s Plastic Packaging Tax and view payments, returns and penalties"
        a.get(20).attr("href") shouldBe "https://www.tax.service.gov.uk/plastic-packaging-tax/account"

        expectedH3(html, "Other tax services", 6)
        html.select(".govuk-warning-text").text shouldBe "! The agent services account is the home for HMRC tax services launched from 2019. For any tax services not listed here, sign out of this account and log in to your HMRC online services for agents account (opens in new tab)."
        a.get(21).attr("href") shouldBe "https://www.gov.uk/government/collections/hmrc-online-services-for-agents#hmrc-online-services-for-agents-account"
        // end of accordion

        // No VAT links
        html.text().contains("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat") shouldBe false
      }

      "agent with showFeatureInvite being false" in {
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))
        givenAuthorisedAsAgentWith(arn)
        givenHidePrivateBetaInvite()

        val controller = appBuilder().build().injector.instanceOf[AgentServicesController]

        val response = await(controller.showAgentServicesAccount()(fakeRequest("GET", "/home")))
        val html = Jsoup.parse(contentAsString(response))

        expectedHomeBannerContent(html)
        expectedClientAuthContent(html, betaInviteContent = false)

        // no beta invite
        html.text().contains("Help improve our new feature") shouldBe false
        html.text().contains("Try out access groups and tell us what you think.") shouldBe false
        html.text().contains("/agent-services-account/private-beta-testing") shouldBe false
        html.text().contains("/private-beta") shouldBe false

      }
    }

  }

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showSuspendedWarning()(fakeRequest("GET", "/home").withSession("suspendedServices" -> "HMRC-MTD-IT,HMRC-MTD-VAT"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("suspension-warning.header"))
      content should include(messagesApi("suspension-warning.p1"))
      content should include(messagesApi("suspension-warning.p2.multi"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-IT"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-VAT"))
      content should include(messagesApi("suspension-warning.p3"))
      content should include(htmlEscapedMessage("suspension-warning.p4"))
      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2Fhome"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
  }

  def verifyContactSection(html: Document) = {
    val contactDetailsSection = html.select("#contact-details")
    contactDetailsSection.select("h2").text shouldBe "Contact details"
    contactDetailsSection.select("p a").text shouldBe "View the contact details we have for your business"
    contactDetailsSection.select("p a").attr("href") shouldBe "/agent-services-account/account-details"
  }

  def verifyClientsSectionNotPresent(html: Document) = {
    html.select("section#manage-clients-section").isEmpty shouldBe true
  }
  def verifyClientsSection(html: Document) = {
    val section = html.select("section#manage-clients-section")
    section.select("h2").text shouldBe "Clients"
    section.select("p").text shouldBe "View client details, update client reference and see what groups a client is in."
    val list = section.select("ul li")
    list.get(0).select("a").text shouldBe "Manage clients"
    list.get(0).select("a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-clients"
    list.get(1).select("a").text shouldBe "Clients who are not in any groups"
    list.get(1).select("a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/unassigned-clients"
    section.select("hr").isEmpty() shouldBe false
  }

  def verifyManageTeamMembersSection(html: Document) = {
    val section = html.select("#manage-team-members-section")
    section.select("h2").text shouldBe "Manage team members’ access groups"
    section.select("a").text shouldBe "Manage team members’ access groups"
    section.select("hr").isEmpty() shouldBe false

  }

  def verifyHowToManageSection(html: Document) = {
    val section = html.select("#how-to-manage-team-members-section")
    section.select("h2").text shouldBe "Manage team members on your agent services account"
    section.select(Css.detailsSummary).text() shouldBe "How to add or remove team members"
    section.select(Css.detailsText).text() shouldBe "Use the link in this section to create a new Government Gateway user ID for your team members. These IDs will be linked to the agent services account. Once your team members have verified their new IDs, you can give them access to the account. You can also remove a team member’s access using the same link."

    val list = section.select("ul li")
    list.get(0).select("a").text shouldBe "Create Government Gateway user IDs for team members (opens in new tab)"
    list.get(0).select("a").attr("href") shouldBe s"http://localhost:1111/user-profile-redirect-frontend/group-profile-management"
    list.get(1).select("a").text shouldBe "Manage team member access to your agent services account (opens in new tab)"
    list.get(1).select("a").attr("href") shouldBe s"http://localhost:1111/tax-and-scheme-management/users?origin=Agent"
    section.select("hr").isEmpty() shouldBe false

  }

  def verifyInfoSection(html: Document, status: String = "on") = {
    val section = html.select("#info-section")
    section.select("h2").text shouldBe "Access groups"
    section.select("p").get(0).text.shouldBe("Access groups allow you to restrict which team members can manage a client’s tax.")
    section.select("p").get(1).text.shouldBe(s"Status Turned $status")
    html.select("#opt-in-status").text shouldBe s"Status Turned $status"
    html.select("#opt-in-status").select("#status-value").text shouldBe s"Turned $status"
  }

  "manage-account" should {

    val manageAccountTitle = "Manage account - Agent services account - GOV.UK"

    "return Status: OK and body containing correct content when gran perms FF disabled" in {

      val controllerWithGranPermsDisabled =
        appBuilder(Map("features.enable-gran-perms" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]

      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithGranPermsDisabled.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but there was an error getting optin-status" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdFailure(Arn(arn))
      givenOptinStatusFailedForArn(Arn(arn))
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but ARN is not on allowed list" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedNotOk() // agent-permissions allowlist
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return status: OK and body containing content for status Opted-In_READY (no access groups created yet)" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty)) // no access groups yet
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      val h2 = html.select(H2)

      val li = html.select(Css.LI)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Access groups"
      html.select("#opt-in-status").text shouldBe "Status Turned on"
      html.select("#opt-in-status").select("#status-value").text shouldBe "Turned on"

      paragraphs.get(0).text
        .shouldBe("Access groups allow you to restrict which team members can manage a client’s tax.")
      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe false
      li.get(0).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/create-group/select-group-type"
      li.get(1).child(0).text shouldBe "Manage access groups"
      li.get(1).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-access-groups"
      li.get(2).child(0).text shouldBe "Turn off access groups"
      li.get(2).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/turn-off-guide"

      verifyClientsSection(html)
      verifyHowToManageSection(html)
      verifyContactSection(html)
    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq(customSummary))) // there is already an access group
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val li = html.select(Css.LI)

      val h2 = html.select(H2)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Access groups"
      html.select("#opt-in-status").text shouldBe "Status Turned on"
      html.select("#opt-in-status").select("#status-value").text shouldBe "Turned on"

      paragraphs.get(0).text
        .shouldBe("Access groups allow you to restrict which team members can manage a client’s tax.")

      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe false

      verifyClientsSection(html)
      verifyManageTeamMembersSection(html)
      verifyHowToManageSection(html)

    }

    "return status: OK and body containing content for status Opted-In_NOT_READY" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInNotReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      html.select(Css.insetText).get(0).text
        .shouldBe("You have added new clients but need to wait until your client details are ready to use with access groups. You will receive a confirmation email after which you can start using access groups.")

      verifyInfoSection(html, "on")
      verifyManageTeamMembersSection(html)
      verifyHowToManageSection(html)
      verifyContactSection(html)
      verifyClientsSectionNotPresent(html)

    }

    "return status: OK and body containing content for status Opted-In_SINGLE_USER" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInSingleUser)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to add more team members to your agent services account under ‘Manage team members’ below.")

      verifyInfoSection(html, "on")
      verifyManageTeamMembersSection(html)
      verifyClientsSectionNotPresent(html)
      verifyHowToManageSection(html)
      verifyContactSection(html)
    }

    "return status: OK and body containing content for status Opted-Out_WRONG_CLIENT_COUNT" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutWrongClientCount)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to have more than one client and fewer than 100,000 clients in your agent services account.")

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyClientsSectionNotPresent(html)
      verifyContactSection(html)

    }

    "return status: OK and body containing content for status Opted-Out_SINGLE_USER" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutSingleUser)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to add more team members to your agent services account under ‘Manage team members’ below.")

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyContactSection(html)
      verifyClientsSectionNotPresent(html)

    }

    "return status: OK and body containing content for status Opted-Out_ELIGIBLE" in {

      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutEligible)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyClientsSectionNotPresent(html)
    }
  }

  "account-details" should {

    "return status OK" when {
      "agent is admin and details found" in {
        givenAuthorisedAsAgentWith(arn)
        givenAgentDetailsFound(
          AgencyDetails(
            Some("My Agency"),
            Some("abc@abc.com"),
            Some(BusinessAddress("25 Any Street", Some("Any Town"), None, None, Some("TF3 4TR"), "GB"))))

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        status(response) shouldBe OK
      }

      "agent is admin and details not found" in {
        givenAuthorisedAsAgentWith(arn)
        givenAgentDetailsNoContent()

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        status(response) shouldBe OK
      }

      "agent is assistant" in {
        givenAuthorisedAsAgentWith(arn, isAdmin = false)
        givenAgentDetailsNoContent()

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        status(response) shouldBe OK
      }
    }

    "display correct content" when {
      "agent is admin and details found" in {
        givenAuthorisedAsAgentWith(arn)
        givenAgentDetailsFound(
          AgencyDetails(
            Some("My Agency"),
            Some("abc@abc.com"),
            Some(BusinessAddress("25 Any Street", Some("Any Town"), None, None, Some("TF3 4TR"), "GB"))))

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        val html = Jsoup.parse(contentAsString(response))

        html.title() shouldBe "Account details - Agent services account - GOV.UK"
        html.select(H1).get(0).text shouldBe "Account details"
        html.select(secondaryNavLinks).get(1).text shouldBe "Manage account New"
        html.select(secondaryNavLinks).get(1).select("span").text shouldBe "Manage account"
        html.select(secondaryNavLinks).get(1).select("strong").text shouldBe "New"

        html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"

        html.select(H2).get(0).text shouldBe "Agent services account details"

        html.select(insetText).text() shouldBe "To change these details you will need to write to us. Find out more by reading the guidance (opens in new tab). You can only change your details if you are a director, company secretary, sole trader, proprietor or partner."
        html.select(link).get(0).attr("href").shouldBe("https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing")

        html.select(summaryListKeys).get(0).text shouldBe "Email address"
        html.select(summaryListValues).get(0).text shouldBe "abc@abc.com"
        html.select(summaryListKeys).get(1).text shouldBe "Name"
        html.select(summaryListValues).get(1).text shouldBe "My Agency"
        html.select(summaryListKeys).get(2).text shouldBe "Address"
        html.select(summaryListValues).get(2).text shouldBe "25 Any Street Any Town TF3 4TR GB"

      }

      "agent is admin and details not found" in {
        givenAuthorisedAsAgentWith(arn)
        givenAgentDetailsNoContent()

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        val html = Jsoup.parse(contentAsString(response))

        html.title() shouldBe "Account details - Agent services account - GOV.UK"
        html.select(H1).get(0).text shouldBe "Account details"

        html.select(secondaryNavLinks).get(1).text shouldBe "Manage account New"
        html.select(secondaryNavLinks).get(1).select("span").text shouldBe "Manage account"
        html.select(secondaryNavLinks).get(1).select("strong").text shouldBe "New"

        html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"

        html.select(H2).get(0).text shouldBe "Agent services account details"

        html.select(insetText).text() shouldBe "To change these details you will need to write to us. Find out more by reading the guidance (opens in new tab). You can only change your details if you are a director, company secretary, sole trader, proprietor or partner."

        html.select(summaryListKeys).get(0).text shouldBe "Email address"
        html.select(summaryListValues).get(0).text shouldBe "None"
        html.select(summaryListKeys).get(1).text shouldBe "Name"
        html.select(summaryListValues).get(1).text shouldBe "None"
        html.select(summaryListKeys).get(2).text shouldBe "Address"
        html.select(summaryListValues).get(2).text shouldBe ""

      }

      "the agent is not Admin" in {
        givenAuthorisedAsAgentWith(arn, isAdmin = false)
        givenAgentDetailsFound(
          AgencyDetails(
            Some("My Agency"),
            Some("abc@abc.com"),
            Some(BusinessAddress("25 Any Street", Some("Any Town"), None, None, Some("TF3 4TR"), "GB"))))

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        val html = Jsoup.parse(contentAsString(response))

        html.title() shouldBe "Account details - Agent services account - GOV.UK"
        html.select(H1).get(0).text shouldBe "Account details"
        // checks isAdmin passed correctly
        html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

        // this is fine because manage-account redirects to /your-account if isAdmin is false
        html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"

        html.select(H2).get(0).text shouldBe "Agent services account details"
        html.select(insetText).text() shouldBe "To change these details you will need to write to us. Find out more by reading the guidance (opens in new tab). You can only change your details if you are a director, company secretary, sole trader, proprietor or partner."

        html.select(summaryListKeys).get(0).text shouldBe "Email address"
        html.select(summaryListValues).get(0).text shouldBe "abc@abc.com"
        html.select(summaryListKeys).get(1).text shouldBe "Name"
        html.select(summaryListValues).get(1).text shouldBe "My Agency"
        html.select(summaryListKeys).get(2).text shouldBe "Address"
        html.select(summaryListValues).get(2).text shouldBe "25 Any Street Any Town TF3 4TR GB"

      }
    }

  }

  val yourAccountUrl: String = routes.AgentServicesController.yourAccount.url

  s"GET on Your Account at url: $yourAccountUrl" should {

    "render correctly for Standard User who's Opted-In_READY without Access Groups" in {
      val providerId = RandomUtils.nextLong().toString
      givenFullAuthorisedAsAgentWith(arn, providerId)
      givenOptinRecordExistsForArn(Arn(arn), exists = true)
      givenAccessGroupsForTeamMember(Arn(arn), providerId, Seq.empty)
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"
      html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email address"
      userDetailsPanel.select(summaryListValues).get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select(summaryListKeys).get(2).text() shouldBe "Role"
      userDetailsPanel.select(summaryListValues).get(2)
        .text() shouldBe "Standard user You can view your assigned access groups and clients. You can also view clients who are not in any access groups."

      val userGroupsPanel = html.select("div#user-groups")
      val grps = userGroupsPanel.select("ul li a")
      grps.isEmpty shouldBe true
      userGroupsPanel.select("p").get(0).text shouldBe "You are not currently assigned to any access groups."

      userGroupsPanel.select("a").get(0).text shouldBe "View other clients"
      userGroupsPanel.select("a").get(0).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/other-clients"

      //BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation’s administrators"
      bottomPanel.select("a").get(0).text shouldBe "View administrators"
      bottomPanel.select("a").get(0).attr("href") shouldBe routes.AgentServicesController.administrators.url
      bottomPanel.select("h2").get(1).text shouldBe "Contact details"
      bottomPanel.select("a").get(1).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.accountDetails.url

    }

    "render correctly for Standard User who's NOT Opted-In_READY without Access Groups" in {
      val providerId = RandomUtils.nextLong().toString
      givenFullAuthorisedAsAgentWith(arn, providerId)
      givenOptinRecordExistsForArn(Arn(arn), exists = false)
      givenAccessGroupsForTeamMember(Arn(arn), providerId, Seq.empty)
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"
      html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email address"
      userDetailsPanel.select(summaryListValues).get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select(summaryListKeys).get(2).text() shouldBe "Role"
      userDetailsPanel.select(summaryListValues).get(2)
        .text() shouldBe "Standard user You can view your assigned access groups and clients. You can also view clients who are not in any access groups."

      val userGroupsPanel = html.select("div#user-groups")
      userGroupsPanel.isEmpty shouldBe true

      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation’s administrators"
      bottomPanel.select("a").get(0).text shouldBe "View administrators"
      bottomPanel.select("a").get(0).attr("href") shouldBe routes.AgentServicesController.administrators.url
      bottomPanel.select("h2").get(1).text shouldBe "Contact details"
      bottomPanel.select("a").get(1).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.accountDetails.url

    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {
      val groupId2 = UUID.randomUUID()

      val providerId = RandomUtils.nextLong().toString
      val groupSummaries: Seq[GroupSummary] = Seq(
        customSummary,
        customSummary.copy(groupId2, "Carrots"),
        taxSummary
      )
      givenFullAuthorisedAsAgentWith(arn, providerId)
      givenOptinRecordExistsForArn(Arn(arn), exists = true)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(groupSummaries)) // there is already an access group
      givenAccessGroupsForTeamMember(Arn(arn), providerId, groupSummaries)
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email address"
      userDetailsPanel.select(summaryListValues).get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select(summaryListKeys).get(2).text() shouldBe "Role"
      userDetailsPanel.select(summaryListValues).get(2)
        .text() shouldBe "Standard user You can view your assigned access groups and clients. You can also view clients who are not in any access groups."

      val userGroupsPanel = html.select("div#user-groups")
      val grps = userGroupsPanel.select("ul li a")
      grps.get(0).text() shouldBe "Potatoes"
      grps.get(0).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/group-clients/custom/${groupId1.toString}"
      grps.get(1).text() shouldBe "Carrots"
      grps.get(1).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/group-clients/custom/${groupId2.toString}"
      grps.get(2).text() shouldBe "TRust me"
      grps.get(2).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/group-clients/tax/${taxSummary.groupId.toString}"
      userGroupsPanel.select("a").get(3).text shouldBe "View other clients"
      userGroupsPanel.select("a").get(3).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/other-clients"

      //BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation’s administrators"
      bottomPanel.select("a").get(0).text shouldBe "View administrators"
      bottomPanel.select("a").get(0).attr("href") shouldBe routes.AgentServicesController.administrators.url
      bottomPanel.select("h2").get(1).text shouldBe "Contact details"
      bottomPanel.select("a").get(1).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.accountDetails.url


    }
  }

  val adminUrl: String = routes.AgentServicesController.administrators.url

  s"GET on Administrators of your account at url: $adminUrl" should {

    "render static data and list of Admin Users for ARN if standard user" in {
      givenAuthorisedAsAgentWith(arn, isAdmin = false)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      val teamMembers = Seq(
        UserDetails(credentialRole = Some("User"), name = Some("Robert Builder"), email = Some("bob@builder.com")),
        UserDetails(credentialRole = Some("User"), name = Some("Steve Smith"), email = Some("steve@builder.com")),
        UserDetails(credentialRole = Some("Admin"), name = Some("Albert Forger"), email = Some("a.forger@builder.com")),
        //Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant")),
      )
      stubGetTeamMembersForArn(Arn(arn), teamMembers)
      val response = await(controller.administrators()(fakeRequest("GET", adminUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Administrators - Agent services account - GOV.UK"
      html.select(Css.H1).get(0).text shouldBe "Administrators"

      html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/your-account"

      html.select(paragraphs).get(0).text shouldBe "Administrators can:"
      html.select(LI).get(0).text shouldBe "turn access groups on or off"
      html.select(LI).get(1).text shouldBe "view information about all clients and access groups"
      html.select(LI).get(2).text shouldBe "create, rename and delete access groups"
      html.select(LI).get(3).text shouldBe "assign clients and team members to access groups"
      val adminNames = html.select(summaryListKeys)
      val adminEmails = html.select(summaryListValues)
      adminNames.size() shouldBe 3
      adminNames.get(0).text shouldBe "Robert Builder"
      adminEmails.get(0).text shouldBe "bob@builder.com"
      adminNames.get(1).text shouldBe "Steve Smith"
      adminEmails.get(1).text shouldBe "steve@builder.com"
      adminNames.get(2).text shouldBe "Albert Forger"
      adminEmails.get(2).text shouldBe "a.forger@builder.com"
    }

    "return forbidden for admin users" in {
      givenAuthorisedAsAgentWith(arn) // isAdmin = true

      val response = await(controller.administrators()(fakeRequest("GET", adminUrl)))

      status(response) shouldBe 403
    }

  }

  "GET help" should {

    "return Status: OK" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      status(response) shouldBe OK
    }

    "contain matching heading in page title" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Help and guidance - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Help and guidance"
    }

    "contain body with correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      val html = Jsoup.parse(contentAsString(response))
      val h2 = html.select(H2)
      val h3 = html.select(H3)
      val p = html.select(paragraphs)
      val li = html.select(LI)
      val a = html.select(link)

      p.get(0).text shouldBe "This guidance is for tax agents and advisors. It describes how to use your agent services account."

      // Accordion tab1
      h2.get(0).text shouldBe "About this guidance"
      p.get(1).text shouldBe "This help and guidance is for tax agents and advisors. It covers how to use your agent services account to:"
      li.get(0).text shouldBe "copy VAT and Income Tax for Self Assessment authorisations from other agent accounts"
      li.get(1).text shouldBe "request and manage authorisations from clients"
      li.get(2).text shouldBe "manage tax services launched since 2019"
      li.get(3).text shouldBe "manage the tax affairs of your assigned clients"
      li.get(4).text shouldBe "view the details we hold about your organisation"
      p.get(2).text shouldBe "If you have administrator access, you can also:"
      li.get(5).text shouldBe "control which staff members can view and manage a client’s tax"
      li.get(6).text shouldBe "manage client references"

      // Accordion tab2
      h2.get(1).text shouldBe "About your agent services account"
      p.get(3).text shouldBe "You can only have one agent services account, but you can add several team members to this account. They will need to have Government Gateway user IDs for you to add them."
      p.get(4).text shouldBe "The ‘Account home’ screen includes links for managing client authorisations and certain tax services. For other tax services, you will need to log into your online services for agents account."
      p.get(5).text shouldBe "The ‘Manage account’ screen will vary, depending on whether you have administrator or standard user access. Administrators can choose to manage access permissions using the new feature ‘access groups’. When access groups are turned on, standard users can only manage the clients they have been assigned to."

      // Accordion tab3
      h2.get(2).text shouldBe "Account home: client authorisations"
      p.get(6).text shouldBe "This section allows you to create, view and manage authorisation requests."
      p.get(7).text shouldBe "You can copy across authorisations for VAT and Income Tax for Self Assessment from your online services for agents accounts."
      p.get(8).text shouldBe "You can also create an authorisation request for a client, and view or manage your recent authorisation requests."
      p.get(9).text shouldBe "When asking a client for authorisation, you can send them the link to this guidance:"
      a.get(0).text shouldBe "https://www.gov.uk/guidance/authorise-an-agent-to-deal-with-certain-tax-services-for-you"
      a.get(0).attr("href") shouldBe "https://www.gov.uk/guidance/authorise-an-agent-to-deal-with-certain-tax-services-for-you"

      // Accordion tab4 - these links are in list items 7-24
      h2.get(3).text shouldBe "Account home: tax services"

      h3.get(0).text shouldBe "VAT"
      a.get(1).text shouldBe "Making Tax Digital for VAT as an agent: step by step"
      a.get(1).attr("href") shouldBe "https://www.gov.uk/guidance/making-tax-digital-for-vat-as-an-agent-step-by-step"
      a.get(2).text shouldBe "How to keep digital records and file returns for Making Tax Digital for VAT"
      a.get(2).attr("href") shouldBe "https://www.gov.uk/government/publications/vat-notice-70022-making-tax-digital-for-vat/vat-notice-70022-making-tax-digital-for-vat"
      a.get(3).text shouldBe "How to sign clients up for Making Tax Digital for VAT"
      a.get(3).attr("href") shouldBe "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-vat"
      a.get(4).text shouldBe "Help and support for Making Tax Digital (videos and webinars)"
      a.get(4).attr("href") shouldBe "https://www.gov.uk/guidance/help-and-support-for-making-tax-digital#creating-an-agent-services-account"

      h3.get(1).text shouldBe "Making Tax Digital for Income Tax for Self Assessment (ITSA)"
      a.get(5).text shouldBe "Making Tax Digital for Income Tax as an agent: step by step"
      a.get(5).attr("href") shouldBe "https://www.gov.uk/government/collections/making-tax-digital-for-income-tax-as-an-agent-step-by-step"
      a.get(6).text shouldBe "Using Making Tax Digital for Income Tax"
      a.get(6).attr("href") shouldBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax"
      a.get(7).text shouldBe "How to sign clients up for Making Tax Digital for Income Tax"
      a.get(7).attr("href") shouldBe "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-income-tax"
      a.get(8).text shouldBe "Help and support for Making Tax Digital (videos and webinars)"
      a.get(8).attr("href") shouldBe "https://www.gov.uk/guidance/help-and-support-for-making-tax-digital#creating-an-agent-services-account"

      h3.get(2).text shouldBe "Trusts and estates"
      a.get(9).text shouldBe "How to register your client’s estate"
      a.get(9).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-estate"
      a.get(10).text shouldBe "How to register your client’s trust"
      a.get(10).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-trust"

      h3.get(3).text shouldBe "Capital Gains Tax on UK property"
      a.get(11).text shouldBe "How to ask for authorisation, manage your client’s account and send returns as an agent"
      a.get(11).attr("href") shouldBe "https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account"

      h3.get(4).text shouldBe "Plastic Packaging Tax"
      a.get(12).text shouldBe "Check if your client needs to register for Plastic Packaging Tax"
      a.get(12).attr("href") shouldBe "https://www.gov.uk/guidance/check-if-you-need-to-register-for-plastic-packaging-tax"
      a.get(13).text shouldBe "Register for the next live webinar"
      a.get(13).attr("href") shouldBe "https://www.gov.uk/guidance/help-and-support-for-agents#plastic-packaging-tax"

      h3.get(5).text shouldBe "Tax services you cannot manage in this account"
      a.get(14).text shouldBe "Self Assessment"
      a.get(14).attr("href") shouldBe "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
      a.get(15).text shouldBe "VAT (not including Making Tax Digital for VAT)"
      a.get(15).attr("href") shouldBe "https://www.gov.uk/guidance/vat-online-services-for-agents"
      a.get(16).text shouldBe "Corporation Tax"
      a.get(16).attr("href") shouldBe "https://www.gov.uk/guidance/corporation-tax-for-agents-online-service"
      a.get(17).text shouldBe "Authorising an agent for other tax services"
      a.get(17).attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview"
      a.get(18).text shouldBe "PAYE for Agents"
      a.get(18).attr("href") shouldBe "https://www.gov.uk/guidance/payecis-for-agents-online-service"
      a.get(19).text shouldBe "How to change or cancel authorisations as an agent"
      a.get(19).attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview#how-to-change-or-cancel-authorisations-as-an-agent"

      // Accordion tab5
      h2.get(4).text shouldBe "Manage account: standard users"
      p.get(10).text shouldBe "Standard users cannot make any changes to access groups. If access groups are turned off, they can manage the tax of all their organisation's clients. When access groups are turned on, they can only:"
      li.get(25).text shouldBe "view the access groups they are assigned to"
      li.get(26).text shouldBe "view and manage clients they are assigned to through these access groups"
      p.get(11).text shouldBe "Whether access groups are on or off, they can also:"
      li.get(27).text shouldBe "view details of the people in the company with administrator access"
      li.get(28).text shouldBe "view the details we hold about their company"
      p.get(12).text shouldBe "If you are a standard user and you need to access more information, contact someone in your company with administrator access."

      // Accordion tab6
      h2.get(5).text shouldBe "Manage account: administrators"
      p.get(13).text shouldBe "Team members with administrator access can:"
      li.get(29).text shouldBe "see all client, team member and access group details"
      li.get(30).text shouldBe "turn access groups on and off"
      li.get(31).text shouldBe "create, edit and delete access groups"
      li.get(32).text shouldBe "assign clients and team members to access groups"
      li.get(33).text shouldBe "create or change client references within this service"
      li.get(34).text shouldBe "view the details we hold about their organisation"
      h3.get(6).text shouldBe "Manage access permissions with access groups new"
      p.get(14).text shouldBe "Access groups allow you to manage access permissions for your team members within your agent services account."
      p.get(15).text shouldBe "By default, all your team members can manage all your clients’ tax affairs. You may want to limit who can manage a specific client’s tax. If so, turn on access groups."
      p.get(16).text shouldBe "Access groups include team members and clients. If a client is in access groups, only the team members in those groups can manage their tax. You can change the clients and team members in a group at any time"
      p.get(17).text shouldBe "You do not need to assign all your clients to groups. If you do not add a client to any access groups, any staff member can manage their tax."
      p.get(18).text shouldBe "To use access groups your agent services account needs to include:"
      li.get(35).text shouldBe "more than one team member"
      li.get(36).text shouldBe "between 2 and 1,000 clients (inclusive)"
      p.get(19).text shouldBe "HMRC are looking into making access groups available to larger agent firms."
      p.get(20).text shouldBe "When you turn access groups on, it may take some time for our service to gather all your client details. If this happens then you will receive an email when the processing is done."
      p.get(21).text shouldBe "If you turn access groups off then all your team members will be able to manage all your clients’ tax again. The service will remember your groups, so you can restore them by turning access groups on again."
      p.get(22).text shouldBe "Access groups do not work with Income Record Viewer at present. HMRC is looking into this."
      h3.get(7).text shouldBe "Manage team members"
      p.get(23).text shouldBe "You cannot add team members to your account within this service. If you select 'Add or remove team members' then the required service will open in a new tab."
      h3.get(8).text shouldBe "Manage clients"
      p.get(24).text shouldBe "You can manage the client’s reference within access groups. This will not affect their details in other Government Gateway services."

    }

  }
}

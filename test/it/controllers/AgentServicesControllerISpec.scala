/*
 * Copyright 2025 HM Revenue & Customs
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

package it.controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.Messages
import play.api.test.Helpers
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import support.BaseISpec
import support.Css._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.{routes => desiDetailsRoutes}
import uk.gov.hmrc.agentservicesaccount.controllers.AgentServicesController
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.GroupSummary
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.UserDetails
import stubs.AgentAssuranceStubs._
import stubs.AgentPermissionsStubs._
import stubs.AgentUserClientDetailsStubs._
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.util.UUID
import scala.util.Random

class AgentServicesControllerISpec
extends BaseISpec {

  val controller: AgentServicesController = inject[AgentServicesController]

  val groupId1: UUID = UUID.randomUUID()

  val customSummary: GroupSummary = GroupSummary(
    groupId1,
    "Potatoes",
    Some(1),
    1
  )
  val taxSummary: GroupSummary = GroupSummary(
    UUID.randomUUID(),
    "TRust me",
    None,
    1,
    Some("HMRC-TERS")
  )

  // private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  "root" should {

    "redirect to agent service account when the user is not suspended" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when user is suspended" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(
        agentRecord.copy(suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ALL")))))
      )

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.SuspendedJourneyController.showSuspendedWarning().url)
    }

    "redirect to suspended warning when user is suspended for AGSV" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(
        agentRecord.copy(suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("AGSV")))))
      )

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.SuspendedJourneyController.showSuspendedWarning().url)
    }

    "redirect to suspended warning when user is suspended for ALL" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(
        agentRecord.copy(suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ALL")))))
      )

      val response = controller.root()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.SuspendedJourneyController.showSuspendedWarning().url)
    }

    "throw an exception when get agent record returns NOT_FOUND for user" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentDetailsErrorResponse(404)

      intercept[UpstreamErrorResponse] {
        await(controller.root()(fakeRequest()))
      }
    }
  }

  "home" should {

    "return status: OK" when {

      "No suspension details on the agent record" in {
        givenAgentRecordFound(
          agentRecord.copy(suspensionDetails = None)
        )

        givenAuthorisedAsAgentWith(arn.value)
        givenHidePrivateBetaInviteNotFound()

        val result = controller.showAgentServicesAccount(fakeRequest())
        status(result) shouldBe OK
      }

      "Agent is suspended should be redirected" in {
        givenAgentRecordFound(
          agentRecord.copy(suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ALL")))))
        )

        givenAuthorisedAsAgentWith(arn.value)
        givenHidePrivateBetaInviteNotFound()

        val response = controller.showAgentServicesAccount()(fakeRequest("GET", "/home"))
        status(response) shouldBe SEE_OTHER
      }

    }

    "return body containing the correct content" when {

      def expectedHomeBannerContent(html: Document): Assertion = {
        expectedH1(html, "Welcome to your agent services account")
        assertPageContainsText(html, "Agent Reference Number: TARN 000 0001")
      }

      def expectedUrBannerContent(html: Document): Assertion = {
        val banner = html.select("#beta-invite-banner")
        banner.select("h2").text shouldBe "Help improve HMRC services"
        banner.select("a").get(0).text shouldBe "Try out our new access groups feature"
        banner.select("a").get(0).attr("href") shouldBe "/agent-services-account/private-beta-testing"
        banner.select("a").get(1).text shouldBe "Sign up to take part in user research (opens in a new tab)"
        banner.select("a").get(1).attr("href") shouldBe "/test-only/agents-ur-banner"
        banner.select("button").get(0).text shouldBe "No thanks I do not want to take part in research"
      }

      def expectedClientAuthContent(
        html: Document,
        betaInviteContent: Boolean = true
      ): Assertion = {
        val clientAuthSection = html.select("#client-authorisation-section")
        expectedH2(html, "Client authorisations")
        clientAuthSection.select("p").text shouldBe "You must ask your client to authorise you through your agent services account before you can access any services. Copy across an old authorisation or create a new one."
        val links = clientAuthSection.select("ul li a")
        links.get(0).text() shouldBe "Ask a client to authorise you"
        links.get(0).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"
        links.get(1).text() shouldBe "Add existing Self Assessment authorisations to your agent services account"
        links.get(1).attr("href") shouldBe "http://localhost:9438/agent-mapping/start"
        links.get(2).text() shouldBe "Manage your authorisation requests from the last 30 days"
        links.get(2).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/manage-authorisation-requests"
        links.get(3).text() shouldBe "Cancel a client’s authorisation"
        links.get(3).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/agent-cancel-authorisation"
      }

      "an authorised agent with no suspension" in {
        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        givenHidePrivateBetaInviteNotFound()

        val response = await(controller.showAgentServicesAccount()(fakeRequest("GET", "/home")))
        val html = Jsoup.parse(contentAsString(response))

        expectedTitle(html, "Welcome to your agent services account - Agent services account - GOV.UK")

        expectedHomeBannerContent(html)
        expectedUrBannerContent(html)
        expectedClientAuthContent(html)

        // accordion includes Income Record Viewer section
        html.select("#tax-services-h2").text() shouldBe "Tax services you can access through this account"
        val accordion = html.select("#tax-services-accordion")
        accordion.select("#tax-services-accordion-heading-1").text() shouldBe "Making Tax Digital for Income Tax"
        accordion.select("#tax-services-accordion-heading-2").text() shouldBe "VAT"
        accordion.select("#tax-services-accordion-heading-3").text() shouldBe "View a client’s Income record"
        accordion.select("#tax-services-accordion-heading-4").text() shouldBe "Trusts and estates"
        accordion.select("#tax-services-accordion-heading-5").text() shouldBe "Capital Gains Tax on UK property"
        accordion.select("#tax-services-accordion-heading-6").text() shouldBe "Country-by-country reports"
        accordion.select("#tax-services-accordion-heading-7").text() shouldBe "Plastic Packaging Tax"
        accordion.select("#tax-services-accordion-heading-8").text() shouldBe "Report Pillar 2 top-up taxes"
        accordion.select("#tax-services-accordion-heading-9").text() shouldBe "Other tax services"

        // Income Tax
        val one = accordion.select("#tax-services-accordion-content-1")
        one.select("h4").get(0).text() shouldBe "Before you start"
        one.select("p").get(0).text() shouldBe "Refer to Making Tax Digital for Income Tax as an agent: step by step (opens in a new tab)."
        one.select("a").get(0).attr("href") shouldBe "https://www.gov.uk/government/collections/making-tax-digital-for-income-tax-as-an-agent-step-by-step"

        one.select("h4").get(1).text() shouldBe "Get client authorisation"
        one.select("li").get(0).text() shouldBe "Add existing Self Assessment authorisations to your agent services account"
        one.select("a").get(1).attr("href") shouldBe "http://localhost:9438/agent-mapping/start"
        one.select("li").get(1).text() shouldBe "Ask a client to authorise you for Making Tax Digital for Income Tax"
        one.select("a").get(2).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"

        one.select("h4").get(2).text() shouldBe "Sign up your clients"
        one.select("a").get(3).text shouldBe "Sign up your clients for Making Tax Digital for Income Tax"
        one.select("a").get(3).attr("href") shouldBe "http://localhost:9081/report-quarterly/income-and-expenses/sign-up/client/"

        one.select("h4").get(3).text() shouldBe "Manage your client’s details"
        one.select("a").get(4).text() shouldBe "Manage Self Assessment details for clients that are already signed up"
        one.select("a").get(4).attr("href") shouldBe "http://localhost:9081/report-quarterly/income-and-expenses/view/agents"

        // VAT
        val two = accordion.select("#tax-services-accordion-content-2")
        two.select("h4").get(0).text() shouldBe "Before you start"
        two.select("h4").get(1).text() shouldBe "Manage your client’s VAT"
        two.select("p").get(0).text shouldBe "You must first get an authorisation from your client."
        two.select("p").get(0).select("a").text shouldBe "You must first get an authorisation from your client."
        two.select("p").get(0).select("a").attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"

        two.select("a").get(1).text shouldBe "Register your client for VAT (opens in a new tab)"
        two.select("a").get(1).attr("href") shouldBe "https://www.tax.service.gov.uk/register-for-vat"
        two.select("a").get(2).text shouldBe "Manage, submit and view your client’s VAT details (opens in a new tab)"
        two.select("a").get(2).attr("href") shouldBe "http://localhost:9149/vat-through-software/representative/client-vat-number"

        // Income Record Viewer
        val three = accordion.select("#tax-services-accordion-content-3")
        three.select("p").get(0).text shouldBe "Access a client’s Income record to help you complete their Self Assessment tax return."
        three.select("p").get(1).text shouldBe "View a client’s Income record"
        three.select("p").get(1).select("a").text() shouldBe "View a client’s Income record"
        three.select("p").get(1).select("a").attr("href") shouldBe "http://localhost:9996/tax-history/select-client"

        // Trusts
        val four = accordion.select("#tax-services-accordion-content-4")
        four.select("h4").get(0).text() shouldBe "Before you start"
        four.select("h4").get(1).text() shouldBe "Manage your client’s trust"

        val fourPs = four.select("p")
        fourPs.get(0).text shouldBe "Before you ask your client to authorise you, you or your client must have registered the trust (opens in a new tab) or estate (opens in a new tab)."
        fourPs.get(0).select("a").get(0).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"
        fourPs.get(0).select("a").get(0).text shouldBe "ask your client to authorise you"
        fourPs.get(0).select("a").get(1).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-trust"
        fourPs.get(0).select("a").get(1).text shouldBe "registered the trust (opens in a new tab)"
        fourPs.get(0).select("a").get(2).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-estate"
        fourPs.get(0).select("a").get(2).text shouldBe "estate (opens in a new tab)"
        fourPs.get(1).text shouldBe "Your client will need to claim the trust or estate."
        fourPs.get(1).select("a").text shouldBe "claim the trust"
        fourPs.get(1).select("a").attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service#how-to-use-the-online-service"

        fourPs.get(2).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register ."
        fourPs.get(
          2
        ).select("a").get(0).text shouldBe "Use this service to update the details of your client’s trust or declare no changes on the trust register"
        fourPs.get(2).select("a").get(0).attr("href") shouldBe "https://www.gov.uk/guidance/manage-your-trusts-registration-service"

        // Capital Gains Tax on UK property
        val five = accordion.select("#tax-services-accordion-content-5")
        five.select("h4").get(0).text() shouldBe "Before you start"
        five.select("h4").get(1).text() shouldBe "Manage a client’s Capital Gains Tax on UK property"
        val fivePs = five.select("p")
        fivePs.get(0).text shouldBe "Your client must first set up a Capital Gains Tax on UK property account (opens in a new tab)"
        fivePs.get(0).select("a").get(0).text shouldBe "set up a Capital Gains Tax on UK property account (opens in a new tab)"
        fivePs.get(0).select("a").get(0).attr("href")
          .shouldBe("https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#before-you-start")
        fivePs.get(1).text shouldBe "They must then authorise you to act on their behalf (opens in a new tab)"
        fivePs.get(1).select("a").get(0).text shouldBe "authorise you to act on their behalf (opens in a new tab)"
        fivePs.get(1).select(
          "a"
        ).get(0).attr("href") shouldBe "https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account#get-authorisation"

        fivePs.get(2).text shouldBe "Report your client’s Capital Gains Tax on UK property and view payments and penalties"
        fivePs.get(2).select("a").get(0).text shouldBe "Report your client’s Capital Gains Tax on UK property and view payments and penalties"
        fivePs.get(2).select("a").get(0).attr("href") shouldBe "https://www.tax.service.gov.uk/capital-gains-tax-uk-property/start"

        // Country by country
        val six = accordion.select("#tax-services-accordion-content-6")
        six.select("h4").get(0).text() shouldBe "Before you start"
        six.select("h4").get(1).text() shouldBe "Manage country-by-country reports"

        val sixPs = six.select("p")
        sixPs.get(0).text shouldBe "You must first get an authorisation from your client. You can do this by requesting an authorisation"
        sixPs.get(0).select("a").get(0).text shouldBe "requesting an authorisation"
        sixPs.get(0).select("a").get(0).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"

        sixPs.get(1).text shouldBe "Manage your clients’ country-by-country reports and your country-by-country agent contact details"
        sixPs.get(1).select("a").get(0).attr("href") shouldBe "https://www.tax.service.gov.uk/send-a-country-by-country-report"

        // Plastic Packaging Tax
        val seven = accordion.select("#tax-services-accordion-content-7")
        seven.select("h4").get(0).text() shouldBe "Before you start"
        seven.select("h4").get(1).text() shouldBe "Manage your client’s Plastic Packaging Tax"

        val sevenPs = seven.select("p")
        sevenPs.get(0).text shouldBe "Your client must first register for Plastic Packaging Tax (opens in a new tab)"
        sevenPs.get(0).select("a").get(0).text shouldBe "register for Plastic Packaging Tax (opens in a new tab)"
        sevenPs.get(0).select("a").get(0).attr("href") shouldBe "https://www.gov.uk/guidance/register-for-plastic-packaging-tax"

        sevenPs.get(1).text shouldBe "They must then authorise you to act on their behalf"
        sevenPs.get(1).select("a").get(0).text shouldBe "authorise you to act on their behalf"
        sevenPs.get(1).select("a").get(0).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"
        sevenPs.get(2).text shouldBe "Report your client’s Plastic Packaging Tax and view payments, returns and penalties"
        sevenPs.get(2).select("a").get(0).text shouldBe "Report your client’s Plastic Packaging Tax and view payments, returns and penalties"
        sevenPs.get(2).select("a").get(0).attr("href") shouldBe "https://www.tax.service.gov.uk/plastic-packaging-tax/account"

        // Pillar2 Tax
        val eight = accordion.select("#tax-services-accordion-content-8")
        eight.select("h4").get(0).text() shouldBe "Before you start"
        eight.select("h4").get(1).text() shouldBe "Manage your client’s Pillar 2 top-up taxes"

        val eightPs = eight.select("p")
        eightPs.get(0).text shouldBe "Your client must first register to Report Pillar 2 top-up taxes."
        eightPs.get(0).select("a").get(0).text shouldBe "register to Report Pillar 2 top-up taxes."
        eightPs.get(0).select("a").get(0).attr("href") shouldBe "https://www.gov.uk/guidance/report-pillar-2-top-up-taxes"

        eightPs.get(1).text shouldBe "You must first get authorisation from your client. You can do this by requesting an authorisation."
        eightPs.get(1).select("a").get(0).text shouldBe "requesting an authorisation."
        eightPs.get(1).select("a").get(0).attr("href") shouldBe "http://localhost:9435/agent-client-relationships/authorisation-request"
        eightPs.get(2).text shouldBe "Report and manage your client’s Pillar 2 top-up taxes"
        eightPs.get(2).select("a").get(0).text shouldBe "Report and manage your client’s Pillar 2 top-up taxes"
        eightPs.get(2).select("a").get(0).attr("href") shouldBe "http://localhost:10053/report-pillar2-top-up-taxes/asa/input-pillar-2-id"
      }

      "agent with showFeatureInvite being false" in {
        givenAgentRecordFound(agentRecord)

        givenAuthorisedAsAgentWith(arn.value)
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

  def verifyYourOrganisationSection(html: Document): Assertion = {
    val contactDetailsSection = html.select("#your-organisation")
    contactDetailsSection.select("h2").text shouldBe "Your organisation"
    val links = contactDetailsSection.select("p a")
    links.get(0).text shouldBe "View or update anti-money laundering supervision details"
    links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    links.get(1).text shouldBe "View or update contact details we have for your business"
    links.get(1).attr("href") shouldBe "/agent-services-account/manage-account/contact-details/view"
    links.get(2).text shouldBe "View administrators"
    links.get(2).attr("href") shouldBe "/agent-services-account/administrators"
  }

  def verifyClientsSectionNotPresent(html: Document): Assertion = {
    html.select("section#manage-clients-section").isEmpty shouldBe true
  }
  def verifyClientsSection(html: Document): Assertion = {
    val section = html.select("section#manage-clients-section")
    section.select("h2").text shouldBe "Clients"
    section.select("p").text shouldBe "View client details, update client reference and see what groups a client is in."
    val list = section.select("ul li")
    list.get(0).select("a").text shouldBe "Manage clients"
    list.get(0).select("a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-clients"
    list.get(1).select("a").text shouldBe "Clients who are not in any groups"
    list.get(1).select("a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/unassigned-clients"
    section.select("hr").isEmpty shouldBe false
  }

  def verifyManageTeamMembersSection(html: Document): Assertion = {
    val section = html.select("#manage-team-members-section")
    section.select("h2").text shouldBe "Manage team members’ access groups"
    section.select("a").text shouldBe "Manage team members’ access groups"
    section.select("hr").isEmpty shouldBe false

  }

  def verifyHowToManageSection(html: Document): Assertion = {
    val section = html.select("#how-to-manage-team-members-section")
    section.select("h2").text shouldBe "Manage team members on your agent services account"
    section.select(detailsSummary).text() shouldBe "How team members access the agent services account"
    section.select(detailsText).text() shouldBe "When you add a team member, you get a temporary password to give to them. We also email them with a new Government Gateway user ID. The new user ID allows the team member to access this agent services account. An administrator can decide what level of access the team member gets to client details, taxes and schemes."

    val list = section.select("ol.govuk-list--number li")
    list.get(0).select("a").text shouldBe "Add, remove and manage team members"
    list.get(0).select("a").attr("href") shouldBe s"http://localhost:1111/user-profile-redirect-frontend/group-profile-management"
    list.get(1).select("a").text shouldBe "Choose which taxes and schemes the team members can access"
    list.get(1).select("a").attr("href") shouldBe s"http://localhost:1111/tax-and-scheme-management/users?origin=Agent"
    section.select("hr").isEmpty shouldBe false

  }

  def verifyInfoSection(
    html: Document,
    status: String = "on"
  ): Assertion = {
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

      val controllerWithGranPermsDisabled = appBuilder(Map("features.enable-gran-perms" -> false))
        .build()
        .injector.instanceOf[AgentServicesController]

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.NoAmlsDetailsUK, None), arn.value)
      val response = controllerWithGranPermsDisabled.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.view-or-update-contact-details"))
      content should include(messagesApi("manage.account.view-or-update-contact-details"))
      content should include(messagesApi("manage.account.amls.add"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but there was an error getting optin-status" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenArnAllowedOk()
      givenSyncEacdFailure(arn)
      givenOptinStatusFailedForArn(arn)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.view-or-update-contact-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but ARN is not on allowed list" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenArnAllowedNotOk() // agent-permissions allowlist
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.view-or-update-contact-details"))
    }

    "return status: OK and body containing content for status Opted-In_READY (no access groups created yet)" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenArnAllowedOk()
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty)) // no access groups yet
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      val h2 = html.select(H2)

      val li = html.select(LI)
      val p = html.select(paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Access groups"
      html.select("#opt-in-status").text shouldBe "Status Turned on"
      html.select("#opt-in-status").select("#status-value").text shouldBe "Turned on"

      p.get(0).text
        .shouldBe("Access groups allow you to restrict which team members can manage a client’s tax.")
      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe false
      li.get(0).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/create-group/select-group-type?origin=manage-account"
      li.get(1).child(0).text shouldBe "Manage access groups"
      li.get(1).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-access-groups"
      li.get(2).child(0).text shouldBe "Turn off access groups"
      li.get(2).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/turn-off-guide"

      verifyClientsSection(html)
      verifyHowToManageSection(html)
      verifyYourOrganisationSection(html)
    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenArnAllowedOk()
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq(customSummary))) // there is already an access group
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val li = html.select(LI)

      val h2 = html.select(H2)
      val p = html.select(paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Access groups"
      html.select("#opt-in-status").text shouldBe "Status Turned on"
      html.select("#opt-in-status").select("#status-value").text shouldBe "Turned on"

      p.get(0).text
        .shouldBe("Access groups allow you to restrict which team members can manage a client’s tax.")

      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe false

      verifyClientsSection(html)
      verifyManageTeamMembersSection(html)
      verifyHowToManageSection(html)

    }

    "return status: OK and body containing content for status Opted-In_NOT_READY" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      html.select(insetText).get(0).text
        .shouldBe(
          "You have turned on access groups but need to wait until your client details are ready to use with access groups. You will receive a confirmation email after which you can start using access groups."
        )

      verifyInfoSection(html, "on")
      verifyManageTeamMembersSection(html)
      verifyHowToManageSection(html)
      verifyYourOrganisationSection(html)
      verifyClientsSectionNotPresent(html)

    }

    "return status: OK and body containing content for status Opted-In_SINGLE_USER" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInSingleUser)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(insetText).get(0).text
        .shouldBe(
          "To use access groups you need to add more team members to your agent services account under ‘Manage team members on your agent services account’."
        )

      verifyInfoSection(html, "on")
      verifyManageTeamMembersSection(html)
      verifyClientsSectionNotPresent(html)
      verifyHowToManageSection(html)
      verifyYourOrganisationSection(html)
    }

    "return status: OK and body containing content for status Opted-Out_WRONG_CLIENT_COUNT" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedOutWrongClientCount)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(insetText).get(0).text
        .shouldBe("To use access groups you need more than 1 client in your agent services account.")

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyClientsSectionNotPresent(html)
      verifyYourOrganisationSection(html)

    }

    "return status: OK and body containing content for status Opted-Out_SINGLE_USER" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedOutSingleUser)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"

      html.select(insetText).get(0).text
        .shouldBe(
          "To use access groups you need to add more team members to your agent services account under ‘Manage team members on your agent services account’."
        )

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyYourOrganisationSection(html)
      verifyClientsSectionNotPresent(html)

    }

    "return status: OK and body containing content for status Opted-Out_ELIGIBLE" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedOutEligible)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      verifyInfoSection(html, "off")
      verifyHowToManageSection(html)
      verifyClientsSectionNotPresent(html)
    }

    "return view AMLS link for ValidAmlsDetailsUK" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "View or update anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return view AMLS link for NoAmlsDetailsNonUK" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.NoAmlsDetailsNonUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "View or update anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return view AMLS link for ValidAmlsNonUK" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsNonUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "View or update anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return view AMLS link for PendingAmlsDetails" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.PendingAmlsDetails, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "View or update anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return add AMLS link for NoAmlsDetailsUK " in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.NoAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "Action: Add anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return add AMLS link for PendingAmlsDetailsRejected " in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.PendingAmlsDetailsRejected, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "Action: Add anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

    "return update AMLS link for ExpiredAmlsDetailsUK" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInNotReady)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(Seq.empty))
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ExpiredAmlsDetailsUK, None), arn.value)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val contactDetailsSection = html.select("#your-organisation")
      contactDetailsSection.select("h2").text shouldBe "Your organisation"
      val links = contactDetailsSection.select("p a")
      links.get(0).text shouldBe "Action: Update anti-money laundering supervision details"
      links.get(0).attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/view-details"
    }

  }

  "account-details" should {

    "return status OK" when {
      "agent is admin and details found" in {
        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        status(response) shouldBe OK
      }

      "agent is assistant and details found" in {
        givenAuthorisedAsAgentWith(arn.value, isAdmin = false)
        givenAgentRecordFound(agentRecord)

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        status(response) shouldBe OK
      }
    }

    "display correct content" when {
      "agent is admin and details found" in {
        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord.copy(agencyDetails =
          Some(AgencyDetails(
            Some("My Agency"),
            Some("abc@abc.com"),
            Some("07345678901"),
            Some(BusinessAddress(
              "25 Any Street",
              Some("Any Town"),
              None,
              None,
              Some("TF3 4TR"),
              "GB"
            ))
          ))
        ))

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        val html = Jsoup.parse(contentAsString(response))

        html.title() shouldBe "Account details - Agent services account - GOV.UK"
        html.select(H1).get(0).text shouldBe "Account details"
        html.select(secondaryNavLinks).get(1).select("span").text shouldBe "Manage account"

        html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"

        html.select(H2).get(0).text shouldBe "Agent services account details"

        html.select(insetText).text() shouldBe "To change these details you will need to write to us. Find out more by reading the guidance (opens in a new tab). You can only change your details if you are a director, company secretary, sole trader, proprietor or partner."
        html.select(
          link
        ).get(0).attr("href").shouldBe("https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing")

        html.select(summaryListKeys).get(0).text shouldBe "Email"
        html.select(summaryListValues).get(0).text shouldBe "abc@abc.com"
        html.select(summaryListKeys).get(1).text shouldBe "Contact telephone number"
        html.select(summaryListValues).get(1).text shouldBe "07345678901"
        html.select(summaryListKeys).get(2).text shouldBe "Name"
        html.select(summaryListValues).get(2).text shouldBe "My Agency"
        html.select(summaryListKeys).get(3).text shouldBe "Address"
        html.select(summaryListValues).get(3).text shouldBe "25 Any Street Any Town TF3 4TR GB"
      }

      "the agent is not Admin" in {
        givenAuthorisedAsAgentWith(arn.value, isAdmin = false)
        givenAgentRecordFound(agentRecord.copy(agencyDetails =
          Some(AgencyDetails(
            Some("My Agency"),
            Some("abc@abc.com"),
            Some("07345678901"),
            Some(BusinessAddress(
              "25 Any Street",
              Some("Any Town"),
              None,
              None,
              Some("TF3 4TR"),
              "GB"
            ))
          ))
        ))

        val response = await(controller.accountDetails().apply(fakeRequest("GET", "/account-details")))
        val html = Jsoup.parse(contentAsString(response))

        html.title() shouldBe "Account details - Agent services account - GOV.UK"
        html.select(H1).get(0).text shouldBe "Account details"
        // checks isAdmin passed correctly
        html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

        // this is fine because manage-account redirects to /your-account if isAdmin is false
        html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"

        html.select(H2).get(0).text shouldBe "Agent services account details"
        html.select(insetText).text() shouldBe "To change these details you will need to write to us. Find out more by reading the guidance (opens in a new tab). You can only change your details if you are a director, company secretary, sole trader, proprietor or partner."

        html.select(summaryListKeys).get(0).text shouldBe "Email"
        html.select(summaryListValues).get(0).text shouldBe "abc@abc.com"
        html.select(summaryListKeys).get(1).text shouldBe "Contact telephone number"
        html.select(summaryListValues).get(1).text shouldBe "07345678901"
        html.select(summaryListKeys).get(2).text shouldBe "Name"
        html.select(summaryListValues).get(2).text shouldBe "My Agency"
        html.select(summaryListKeys).get(3).text shouldBe "Address"
        html.select(summaryListValues).get(3).text shouldBe "25 Any Street Any Town TF3 4TR GB"

      }
    }

  }

  val yourAccountUrl: String = routes.AgentServicesController.yourAccount.url

  s"GET on Your Account at url: $yourAccountUrl" should {

    "render correctly for Standard User who’s Opted-In_READY without Access Groups" in {
      val providerId = Random.nextLong().toString
      givenFullAuthorisedAsAgentWith(arn.value, providerId)
      givenAgentRecordFound(agentRecord)
      givenOptinRecordExistsForArn(arn, exists = true)
      givenAccessGroupsForTeamMember(
        arn,
        providerId,
        Seq.empty
      )
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"
      html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email"
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

      // BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation"
      bottomPanel.select("a").get(0).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(0).attr("href") shouldBe desiDetailsRoutes.ViewContactDetailsController.showPage.url
      bottomPanel.select("a").get(1).text shouldBe "View administrators"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.administrators.url

    }

    "render correctly for Standard User who’s NOT Opted-In_READY without Access Groups" in {
      val providerId = Random.nextLong().toString
      givenFullAuthorisedAsAgentWith(arn.value, providerId)
      givenAgentRecordFound(agentRecord)
      givenOptinRecordExistsForArn(arn, exists = false)
      givenAccessGroupsForTeamMember(
        arn,
        providerId,
        Seq.empty
      )
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"
      html.select(secondaryNavLinks).get(1).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email"
      userDetailsPanel.select(summaryListValues).get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select(summaryListKeys).get(2).text() shouldBe "Role"
      userDetailsPanel.select(summaryListValues).get(2)
        .text() shouldBe "Standard user You can view your assigned access groups and clients. You can also view clients who are not in any access groups."

      val userGroupsPanel = html.select("div#user-groups")
      userGroupsPanel.isEmpty shouldBe true

      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation"
      bottomPanel.select("a").get(0).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(0).attr("href") shouldBe desiDetailsRoutes.ViewContactDetailsController.showPage.url
      bottomPanel.select("a").get(1).text shouldBe "View administrators"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.administrators.url

    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {
      val groupId2 = UUID.randomUUID()

      val providerId = Random.nextLong().toString
      val groupSummaries: Seq[GroupSummary] = Seq(
        customSummary,
        customSummary.copy(groupId2, "Carrots"),
        taxSummary
      )
      givenFullAuthorisedAsAgentWith(arn.value, providerId)
      givenOptinRecordExistsForArn(arn, exists = true)
      givenAgentRecordFound(agentRecord)
      givenAccessGroupsForArn(arn, AccessGroupSummaries(groupSummaries)) // there is already an access group
      givenAccessGroupsForTeamMember(
        arn,
        providerId,
        groupSummaries
      )
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"

      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select(summaryListKeys).get(0).text() shouldBe "Name"
      userDetailsPanel.select(summaryListValues).get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select(summaryListKeys).get(1).text() shouldBe "Email"
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

      // BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your organisation"
      bottomPanel.select("a").get(0).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(0).attr("href") shouldBe desiDetailsRoutes.ViewContactDetailsController.showPage.url
      bottomPanel.select("a").get(1).text shouldBe "View administrators"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.administrators.url

    }

  }

  val adminUrl: String = routes.AgentServicesController.administrators.url

  s"GET on Administrators at url: $adminUrl" should {

    "render static data and list of Admin Users for ARN if standard user" in {
      givenAuthorisedAsAgentWith(arn.value, isAdmin = false)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInReady)
      val teamMembers = Seq(
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Robert Builder"),
          email = Some("bob@builder.com")
        ),
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Steve Smith"),
          email = Some("steve@builder.com")
        ),
        UserDetails(
          credentialRole = Some("Admin"),
          name = Some("Albert Forger"),
          email = Some("a.forger@builder.com")
        ),
        // Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant"))
      )
      stubGetTeamMembersForArn(arn, teamMembers)
      val response = await(controller.administrators()(fakeRequest("GET", adminUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Administrators - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Administrators"

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

    "allow admin users" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenArnAllowedOk()
      givenAgentRecordFound(agentRecord)
      givenSyncEacdSuccess(arn)
      val teamMembers = Seq(
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Robert Builder"),
          email = Some("bob@builder.com")
        ),
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Steve Smith"),
          email = Some("steve@builder.com")
        ),
        UserDetails(
          credentialRole = Some("Admin"),
          name = Some("Albert Forger"),
          email = Some("a.forger@builder.com")
        ),
        // Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant"))
      )
      stubGetTeamMembersForArn(arn, teamMembers)
      givenOptinStatusSuccessReturnsForArn(arn, accessgroups.OptedInReady)

      val response = await(controller.administrators()(fakeRequest("GET", adminUrl)))

      status(response) shouldBe 200
      val html = Jsoup.parse(contentAsString(response))
      html.select(backLink).get(0).attr("href") shouldBe "/agent-services-account/manage-account"
    }
  }

  "GET help" should {

    "return Status: OK" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      status(response) shouldBe OK
    }

    "contain matching heading in page title" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Help and guidance - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Help and guidance"
    }

    "contain body with correct content" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
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
      li.get(0).text shouldBe "copy Income Tax for Self Assessment authorisations from other agent accounts"
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
      p.get(7).text shouldBe "You can copy across authorisations for Income Tax for Self Assessment from your online services for agents accounts."
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
      a.get(2).attr(
        "href"
      ) shouldBe "https://www.gov.uk/government/publications/vat-notice-70022-making-tax-digital-for-vat/vat-notice-70022-making-tax-digital-for-vat"
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
      p.get(10).text shouldBe "Standard users cannot make any changes to access groups. If access groups are turned off, they can manage the tax of all their organisation’s clients. When access groups are turned on, they can only:"
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
      p.get(23).text shouldBe "You cannot add team members to your account within this service. If you select ‘Add or remove team members’ then the required service will open in a new tab."
      h3.get(8).text shouldBe "Manage clients"
      p.get(
        24
      ).text shouldBe "You can manage the client’s reference within access groups. This will not affect their details in other Government Gateway services."

    }

  }

}

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


import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Session
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.{SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

class AgentServicesControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: AgentServicesController = app.injector.instanceOf[AgentServicesController]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  "root" should {
    "redirect to agent services account when suspension is disabled" in {
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithSuspensionDisabled.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to agent service account when suspension is enabled but user is not suspended" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when suspension is enabled and user is suspended" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
    }

    "redirect to suspended warning when suspension is enabled and user is suspended for AGSV" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "redirect to suspended warning when suspension is enabled and user is suspended for ALL" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ALL"))))

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "throw an exception when suspension is enabled and suspension status returns NOT_FOUND for user" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentRecordNotFound


      intercept[SuspensionDetailsNotFound]{
        await(controller.root()(FakeRequest("GET", "/")))
      }
    }
  }

  "home" should {
    "return Status: OK and body containing correct content" in {
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controllerWithSuspensionDisabled.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
      content should include(messagesApi("agent.accountNumber","TARN 000 0001"))
      content should include(messagesApi("agent.services.account.sectionITSA.h2"))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.h3"))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.p", appConfig.agentMappingUrl, appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.link"))
      content should include(messagesApi("agent.services.account.sectionITSA.col2.h3"))
      content should include(appConfig.incomeTaxSubscriptionAgentFrontendUrl)
      content should include("https://www.gov.uk/guidance/follow-the-rules-for-making-tax-digital-for-income-tax#who-can-follow-the-rules")
      content should include(messagesApi("agent.services.account.sectionITSA.col2.link1.text"))
      content should include(messagesApi("agent.services.account.sectionITSA.col2.link2.text"))
      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.col1.h3"))
      content should include(messagesApi("agent.services.account.section1.col1.p", appConfig.agentMappingUrl, appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.h3"))
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.link"))
      content should include(messagesApi("agent.services.account.section3.col1.h2"))
      content should include(messagesApi("agent.services.account.section3.col1.h3"))
      content should include(messagesApi("agent.services.account.section3.col1.text1"))
      content should include(messagesApi("agent.services.account.section3.col1.link1", messagesApi("agent.services.account.section3.col1.link1.href")))
      content should include(messagesApi("agent.services.account.section3.col1.text2"))
      content should include(messagesApi("agent.services.account.section3.col1.link2", messagesApi("agent.services.account.section3.col1.link2.href")))
      content should include(messagesApi("agent.services.account.section3.col2.h3"))
      content should include(messagesApi("agent.services.account.section3.col2.link1", messagesApi("agent.services.account.section3.col2.link1.href")))
      content should include(messagesApi("agent.services.account.section4.h2"))
      content should include(messagesApi("agent.services.account.section4.col1.h3"))
      content should include(messagesApi("agent.services.account.section4.col1.link"))
      content should include(appConfig.agentInvitationsFrontendUrl)
      content should include(appConfig.agentInvitationsFrontendClientTypeUrl)
      content should include(messagesApi("agent.services.account.section4.col2.link1"))
      content should include(messagesApi("agent.services.account.section4.col2.link2"))
      content should include(htmlEscapedMessage("agent.services.account.section4.col2.link3"))
      content should include(appConfig.agentInvitationsTrackUrl)
      content should include(appConfig.agentMappingUrl)
      content should include(appConfig.agentInvitationsCancelAuthUrl)
      content should include(messagesApi("agent.services.account.trusts-section.h2"))
      content should include(messagesApi("agent.services.account.trusts-section.col1.h3"))
      content should include(messagesApi("agent.services.account.trusts-section.col1.register-trust.p", appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include(messagesApi("agent.services.account.trusts-section.col1.register-estate.p"))
      content should include(messagesApi("agent.services.account.trusts-section.col2.h3"))
      content should include(messagesApi("agent.services.account.trusts-section.col2.register-trust-link.text"))
      content should include(messagesApi("agent.services.account.welcome"))
      content should include(messagesApi("agent.services.account.client-authorisations.p"))
      content should include(messagesApi("agent.services.account.tax-services"))
      content should include(messagesApi("asa.other.heading"))
      content should include(messagesApi("asa.other.p1"))
      content should include(messagesApi("serviceinfo.help"))
    }

    "not show Help and Guidance link when toggled off" in {
      val controllerWithHelpToggledOff =
        appBuilder(Map("features.enable-help-and-guidance" -> false)).build().injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controllerWithHelpToggledOff.showAgentServicesAccount()(FakeRequest("GET", "/home"))
      Helpers.contentAsString(response) should not include messagesApi("serviceinfo.help")
    }

    "return Status: OK and body containing correct content when suspension details are in the session and agent is suspended for VATC" in {
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.suspended.h3"))
      content should include(messagesApi("agent.services.account.section1.suspended.p1"))
      content should include(messagesApi("agent.services.account.section1.suspended.p2"))

      content should not include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
    }

    "return Status: OK and body containing correct content when agent suspension is not enabled" in {
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithSuspensionDisabled.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
    }

    "do not fail without continue url parameter" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.showAgentServicesAccount().apply(FakeRequest("GET", "/home"))
      status(response) shouldBe OK
      Helpers.contentAsString(response) should {
        not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
      }
    }

    "include the Income Record Viewer section " in {
      val controller =
        appBuilder(Map("features.enable-agent-suspension" -> false, "features.enable-irv-allowlist" -> true))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)

      val result = controller.showAgentServicesAccount(FakeRequest())
      status(result) shouldBe OK

      val content = Helpers.contentAsString(result)
      content should include (messagesApi("agent.services.account.paye-section.h2"))
    }

  }

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showSuspendedWarning()(FakeRequest("GET", "/home").withSession("suspendedServices" -> "HMRC-MTD-IT,HMRC-MTD-VAT"))

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
      content should include(messagesApi("suspension-warning.button"))
    }
  }

  "manage-account" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.manageAccount().apply(FakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.heading"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))

    }
  }

  "account-details" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentDetailsFound(
        AgencyDetails(
          Some("My Agency"),
          Some("abc@abc.com"),
            Some(BusinessAddress("25 Any Street", Some("Any Town"), None, None, Some("TF3 4TR"), "GB"))))

      val response = controller.accountDetails().apply(FakeRequest("GET", "/account-details"))

      Helpers.status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.summary-list.header"))
      content should include(messagesApi("account-details.summary-list.email"))
      content should include(messagesApi("account-details.summary-list.name"))
      content should include(messagesApi("account-details.summary-list.address"))
      content should include("My Agency")
      content should include("abc@abc.com")
      content should include("25 Any Street")
      content should include("Any Town")
      content should include("TF3 4TR")
      content should include("GB")
      content should include(" To change these details you will need to write to us. <a class=\"govuk-link\" href=https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing target=\"_blank\" rel=\"noreferrer noopener\">Find out more by reading the guidance (opens in new tab)</a>. You can only change your details if you are a director, company secretary, sole trader, proprietor or partner.")

    }

    "return Status: OK and body containing None in place of missing agency details" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentDetailsNoContent()

      val response = controller.accountDetails().apply(FakeRequest("GET", "/account-details"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.summary-list.header"))
      content should include(messagesApi("account-details.summary-list.email"))
      content should include(messagesApi("account-details.summary-list.name"))
      content should include(messagesApi("account-details.summary-list.address"))
      content should include(messagesApi("account-details.summary-list.none"))
      content should include(" To change these details you will need to write to us. <a class=\"govuk-link\" href=https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing target=\"_blank\" rel=\"noreferrer noopener\">Find out more by reading the guidance (opens in new tab)</a>. You can only change your details if you are a director, company secretary, sole trader, proprietor or partner.")

      }

    "return Forbidden if the agent is not Admin" in {
      givenAuthorisedAsAgentWith(arn, isAdmin = false)

      val response = controller.accountDetails().apply(FakeRequest("GET", "/account-details"))
      status(response) shouldBe 403
    }
  }

  "help" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showHelp().apply(FakeRequest("GET", "/help"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("help.title"))
      content should include(messagesApi("help.heading"))
      content should include(messagesApi("help.p1"))
      content should include(messagesApi("help.authorised.h2"))
      content should include(messagesApi("help.authorised.link"))
      content should include(messagesApi("help.mtd.h2"))
      content should include(messagesApi("help.mtd.link1"))
      content should include(messagesApi("help.mtd.link2"))
      content should include(messagesApi("help.mtd.link3"))
      content should include(messagesApi("help.mtd.link4"))
      content should include(messagesApi("help.mtd.link5"))
      content should include(messagesApi("help.trusts.h2"))
      content should include(messagesApi("help.trusts.link1"))
      content should include(messagesApi("help.trusts.link2"))
      content should include(messagesApi("help.cgt.h2"))
      content should include(messagesApi("help.cgt.link"))
      content should include(messagesApi("help.cannot.h2"))
      content should include(messagesApi("help.cannot.link1"))
      content should include(messagesApi("help.cannot.link2"))
      content should include(messagesApi("help.cannot.link3"))
      content should include(messagesApi("help.cannot.link4"))
      content should include(messagesApi("help.cannot.link5"))
      content should include(messagesApi("help.cannot.link6"))
    }
  }
}

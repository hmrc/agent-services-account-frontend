/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

class AgentServicesControllerSpec extends BaseISpec {

  implicit val lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller = app.injector.instanceOf[AgentServicesController]
  implicit val appConfig = app.injector.instanceOf[AppConfig]

  val arn = "TARN0000001"
  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  "root" should {
    "redirect to agent services account when suspension is disabled" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to agent service account when suspension is enabled but user is not suspended" in {
      val controllerWithSuspensionEnabled =
        appBuilder(Map("features.enable-agent-suspension" -> true))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controllerWithSuspensionEnabled.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when suspension is enabled and user is suspended" in {
     val controllerWithSuspensionEnabled =
       appBuilder(Map("features.enable-agent-suspension" -> true))
         .build()
         .injector.instanceOf[AgentServicesController]

      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))

      val response = controllerWithSuspensionEnabled.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
    }

    "throw an exception when suspension is enabled and suspension status returns NOT_FOUND for user" in {
      val controllerWithSuspensionEnabled =
        appBuilder(Map("features.enable-agent-suspension" -> true))
          .build()
          .injector.instanceOf[AgentServicesController]

      givenAuthorisedAsAgentWith(arn)
      givenAgentRecordNotFound


      intercept[SuspensionDetailsNotFound]{
        await(controllerWithSuspensionEnabled.root()(FakeRequest("GET", "/")))
      }
    }
  }

  "home" should {
    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

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
      contentAsString(response) should not include messagesApi("serviceinfo.help")
    }

    "return Status: OK and body containing correct content when suspension details are in the session and agent is suspended for VATC" in {
      val controllerWithSuspensionEnabled =
        appBuilder(Map("features.enable-agent-suspension" -> true))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithSuspensionEnabled.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.suspended.h3"))
      content should include(messagesApi("agent.services.account.section1.suspended.p1"))
      content should include(messagesApi("agent.services.account.section1.suspended.p2"))

      content should not include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
    }

    "return Status: OK and body containing correct content when agent suspension is not enabled" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
    }

    "do not fail without continue url parameter" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.showAgentServicesAccount().apply(FakeRequest("GET", "/home"))
      status(response) shouldBe OK
      contentAsString(response) should {
        not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
      }
    }

    "include the Income Record Viewer section when the IRV allowlist is enabled and the ARN is allowed" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)

      val controller = appBuilder(Map("features.enable-irv-allowlist" -> true)).build().injector.instanceOf[AgentServicesController]

      val result = controller.showAgentServicesAccount(FakeRequest())
      status(result) shouldBe OK

      val content = contentAsString(result)
      content should include (messagesApi("agent.services.account.paye-section.h2"))
    }

    "not include the Income Record Viewer section when the IRV allowlist is enabled and the ARN is not allowed" in {
      givenArnIsNotAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)

      val controller = appBuilder(Map("features.enable-irv-allowlist" -> true)).build().injector.instanceOf[AgentServicesController]

      val result = controller.showAgentServicesAccount(FakeRequest())
      status(result) shouldBe OK

      val content = contentAsString(result)
      content should not include messagesApi("agent.services.account.paye-section.h2")
    }
  }

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showSuspendedWarning()(FakeRequest("GET", "/home").withSession("suspendedServices" -> "HMRC-MTD-IT,HMRC-MTD-VAT"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

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
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
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

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.inset"))
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
    }

    "return Status: OK and body containing None in place of missing agency details" in {
      givenAuthorisedAsAgentWith(arn)
      givenAgentDetailsNoContent()

      val response = controller.accountDetails().apply(FakeRequest("GET", "/account-details"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.inset"))
      content should include(messagesApi("account-details.summary-list.header"))
      content should include(messagesApi("account-details.summary-list.email"))
      content should include(messagesApi("account-details.summary-list.name"))
      content should include(messagesApi("account-details.summary-list.address"))
      content should include(messagesApi("account-details.summary-list.none"))
    }

    "return Forbidden if the agent is not Admin" in {
      givenAuthorisedAsAgentWith(arn, false)

      val response = controller.accountDetails().apply(FakeRequest("GET", "/account-details"))
      status(response) shouldBe 403
    }
  }

  "help" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showHelp().apply(FakeRequest("GET", "/help"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
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

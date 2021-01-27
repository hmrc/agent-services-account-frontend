package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.test.Injecting
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class AfiRelationshipConnectorSpec extends BaseISpec with Injecting {

  val connector = inject[AfiRelationshipConnector]

  "checkIrvAllowed" should {
    "return true when agent-fi-relationship returns 204 No Content" in {
      val arn = Arn("TARN0000001")
      givenArnIsAllowlistedForIrv(arn)
      await(connector.checkIrvAllowed(arn)(HeaderCarrier())) shouldBe true
    }

    "return false when agent-fi-relationship returns 404 Not Found" in {
      val arn = Arn("TARN0000001")
      givenArnIsNotAllowlistedForIrv(arn)
      await(connector.checkIrvAllowed(arn)(HeaderCarrier())) shouldBe false
    }
  }
}

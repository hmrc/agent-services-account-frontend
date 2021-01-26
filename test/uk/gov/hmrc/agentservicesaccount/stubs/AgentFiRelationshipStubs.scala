package uk.gov.hmrc.agentservicesaccount.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{get, noContent, notFound, stubFor}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

object AgentFiRelationshipStubs {
  def givenArnIsAllowlistedForIrv(arn: Arn) =
    stubFor(get(s"/agent-fi-relationship/${arn.value}/irv-allowed").willReturn(noContent()))

  def givenArnIsNotAllowlistedForIrv(arn: Arn) =
    stubFor(get(s"/agent-fi-relationship/${arn.value}/irv-allowed").willReturn(notFound()))
}

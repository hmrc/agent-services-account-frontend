package wiring

import uk.gov.hmrc.play.http.ws.WSHttp

class WSVerbs extends WSHttp {
  override val hooks = NoneRequired
}
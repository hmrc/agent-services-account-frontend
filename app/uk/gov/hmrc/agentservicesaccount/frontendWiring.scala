package uk.gov.hmrc.agentservicesaccount

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.inject.DefaultServicesConfig
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.HttpPost
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

object FrontendAuditConnector extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode {
  override val hooks = NoneRequired
}

class FrontendAuthConnector @Inject()(
  defaultServicesConfig: DefaultServicesConfig,
  val http: HttpPost) extends PlayAuthConnector {

  override val serviceUrl: String = defaultServicesConfig.baseUrl("auth")
}


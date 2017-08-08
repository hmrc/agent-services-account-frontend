import javax.inject.Inject

import akka.stream.Materializer
import com.kenshoo.play.metrics.MetricsFilter
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.http.DefaultHttpFilters
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.filter.{AuthorisationFilter, FilterConfig}
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.inject.{DefaultServicesConfig, RunMode}
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import wiring.WSVerbs

import scala.concurrent.ExecutionContext

/**
  * Defines the filters that are added to the application by extending the default Play filters
  *
  * @param loggingFilter - used to log details of any http requests hitting the service
  * @param auditFilter   - used to call the datastream microservice and publish auditing events
  * @param metricsFilter - used to collect metrics and statistics relating to the service
  * @param authFilter    - used to add authorisation to endpoints in the service if required
  */
class Filters @Inject()(loggingFilter: LoggingFilter, auditFilter: MicroserviceAuditFilter, metricsFilter: MetricsFilter,
                        authFilter: MicroserviceAuthFilter)
  extends DefaultHttpFilters(loggingFilter, auditFilter, metricsFilter, authFilter)

class LoggingFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, configuration: Configuration) extends FrontendLoggingFilter {
  override def controllerNeedsLogging(controllerName: String): Boolean = configuration.getBoolean(s"controllers.$controllerName.needsLogging").getOrElse(true)
}

class MicroserviceAuditFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext,
                                        configuration: Configuration, val auditConnector: MicroserviceAuditConnector) extends FrontendAuditFilter {

  override lazy val maskedFormFields = Seq("password")

  override def controllerNeedsAuditing(controllerName: String): Boolean = configuration.getBoolean(s"controllers.$controllerName.needsAuditing").getOrElse(true)

  override def appName: String = configuration.getString("appName").get

  override def applicationPort: Option[Int] = None
}

class MicroserviceAuditConnector @Inject()(val environment: Environment) extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

class MicroserviceAuthFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext,
                                       configuration: Configuration, val connector: AuthConn) extends AuthorisationFilter {
  override def config: FilterConfig = FilterConfig(configuration.underlying.as[Config]("controllers"))
}

class AuthConn @Inject()(defaultServicesConfig: DefaultServicesConfig,
                         val http: WSVerbs) extends PlayAuthConnector {

  override val serviceUrl: String = defaultServicesConfig.baseUrl("auth")
}


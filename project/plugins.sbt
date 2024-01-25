resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")


addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"        % "3.19.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"    % "2.2.0")
addSbtPlugin("com.typesafe.play"    % "sbt-plugin"            % "2.8.20")
addSbtPlugin("com.typesafe.sbt"     % "sbt-twirl"             % "1.5.1")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"           % "0.3.4")  // provides sbt command "dependencyUpdates"
addSbtPlugin("io.github.irundaia"   % "sbt-sassify"           % "1.5.2")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"         % "2.0.7")

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
addDependencyTreePlugin


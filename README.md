# agent-services-account-frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-services-account-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agent-services-account-frontend/_latestVersion)
## What the service does

This is the frontend for the Agent Services account page. It is available to agents who have the HMRC-AS-AGENT enrolment, 
allowing them to access to a range of HMRC digital services.

### Running the tests

    sbt test

### Running the tests with coverage

    sbt clean coverageOn test coverageReport

### Running the app locally


#### BE services needed to view ASA dashboard

| **Microservice** | **Purpose**  | 
|------------------|--------------|
| agent-client-authorisation  | suspension check        | 
| agent-permissions  | private beta invite check, also granular permissions in manage account         | 

    sm --start AGENT_ONBOARDING -r
    sm --stop AGENT_SERVICES_ACCOUNT_FRONTEND
    sbt run

It should then be listening on port 9401

    browse http://localhost:9401/agent-services-account

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
 

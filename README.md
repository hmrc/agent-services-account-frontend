# agent-services-account-frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-services-account-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agent-services-account-frontend/_latestVersion)
## What the service does

This is the frontend for the Agent Services account page. It is available to agents who have the HMRC-AS-AGENT enrolment, 
allowing them to access to a range of HMRC digital services.

### Manage account vs Your account
ASA accounts typically hold `credentialRole: "User"`. An agency has at least one admin - historically this login has been shared in smaller agencies.

If they create additional users (creating fresh Government Gateway credentials) through the external service they will be assigned to the same group (same ARN). The additional users can be:
- "Admin" - credentialRole is User or Admin, Admin is deprecated
- "Standard" - credentialRole is Assistant

Administrators have access to most ASA functionality, such as sending authorisation requests and managing access groups. They see "Manage account" in the secondary nav bar.

Standard users have limited functionality. They can view the clients lists they are assigned to, and a list of administrators for their agency. They can manage the taxes of clients they have access to. They see "Your account" in the secondary nav bar.

#### Creating additional users (team members)

The link to add users to the agency is in `application.conf` under `user-management.add-user`. It is a redirect through Secure Credentials Platform (SCP). 

## Running the tests

    sbt test

### Running the tests with coverage

    sbt clean coverageOn test coverageReport

### Running the app locally


#### BE services needed to view ASA dashboard

| **Microservice**           | **Purpose**                                                            | 
|----------------------------|------------------------------------------------------------------------|
| agent-client-authorisation | suspension check                                                       | 
| agent-permissions          | private beta invite check, also granular permissions in manage account | 

agent-assurance is not needed for dashboard, but required for the update AMLS details feature via manage account


    sm2 --start AGENT_GRAN_PERMS -r
    sm2 --stop AGENT_SERVICES_ACCOUNT_FRONTEND
    sbt run

It should then be listening on port 9401

    browse http://localhost:9401/agent-services-account

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
 

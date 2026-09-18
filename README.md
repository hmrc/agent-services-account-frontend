# agent-services-account-frontend


## What the service does

This is the frontend for the Agent Services account page. It is available to agents who have the HMRC-AS-AGENT enrolment. The service provides the following functionality:
- Provides links to other HMRC services for agents, such as:
  - MTD Value Added Tax (VAT)
  - Plastic Packaging Tax (PPT)
  - Capital Gains Tax for Property Disposal (CGT-PD)
  - Making Tax Digital for Income Tax (MTD ITSA)
  - Income Record Viewer (IRV)
  - Trusts and Estates (TRS)
  - Country-by-Country Reporting (CBC)
  - Pillar2 (PLR)
- Provides a way for agents to update their anti-money laundering supervision (AMLS) details. 
- Provides a way for agents to update their designatory details.
- Provides a way to turn on Access Groups (if the eligibility criteria are met) and manage them.

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

### Automated testing
This service is tested by the following automated test repositories:
- [agent-services-account-ui-tests](https://github.com/hmrc/agent-services-account-ui-tests)
- [agent-gran-perms-acceptance-tests](https://github.com/hmrc/agent-gran-perms-acceptance-tests/)
- [agent-services-performance-tests](https://github.com/hmrc/agent-services-account-performance-tests)

### Running the app locally


#### BE services needed to view ASA dashboard

| **Microservice**       | **Purpose**                                                            | 
|------------------------|------------------------------------------------------------------------|
| agent-services-account | suspension check and agent details                                     | 
| agent-permissions      | private beta invite check, also granular permissions in manage account | 


    sm2 --start AGENT_GRAN_PERMS -r
    sm2 --stop AGENT_SERVICES_ACCOUNT_FRONTEND
    sbt run

It should then be listening on port 9401

    browse http://localhost:9401/agent-services-account

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
 

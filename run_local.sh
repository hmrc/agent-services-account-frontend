#!/bin/sh

echo "-------------------------------------------------------------"
echo "-- RUNNING UPSTREAM SERVICES LOCALLY USING SERVICE MANAGER --"
echo "-------------------------------------------------------------"
sm --start AGENT_MTD -f
sm --stop AGENT_SERVICES_ACCOUNT_FRONTEND
echo "-------------------------------------------------------------"
echo "--        RUNNING AGENT SERVICES ACCOUNT FRONTEND          --"
echo "-------------------------------------------------------------"
sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run


#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is executed on startup of Docker container.
# (execution of docker run cmd) starts couchdb, postgres and tomcat.
# -----------------------------------------------------------------------------

start_sw360() {
  /etc/init.d/couchdb restart
  /etc/init.d/postgresql restart
  cd /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/bin/
  rm -rf ./indexes/*
  ./startup.sh
  tail_logs
}

stop_sw360() {
  /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/bin/shutdown.sh
  tail_logs
  pkill -9 -f tomcat
  cd /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/webapps/
  rm -rf *.war
  /etc/init.d/couchdb stop
  /etc/init.d/postgresql stop
}

tail_logs()
{
  tail -f --lines=500 /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/logs/catalina.out &
  read -r user_input
  pkill -9 -f tail
}

start_sw360
stop_sw360

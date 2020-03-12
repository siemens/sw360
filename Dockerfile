#
# Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

FROM maven:3.6.1-jdk-8-slim as builder

WORKDIR /app/build/sw360

COPY . .

RUN ./scripts/install-thrift.sh

RUN apt-get install git -y --no-install-recommends \
 && apt-get install wget -y --no-install-recommends

RUN mvn -s /app/build/sw360/docker-config/mvn-proxy-settings.xml clean package -P deploy -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* -DfailIfNoTests=false -Dbase.deploy.dir=. -Dliferay.deploy.dir=/app/build/sw360/deployables/deploy -Dbackend.deploy.dir=/app/build/sw360/deployables/webapps -Drest.deploy.dir=/app/build/sw360/deployables/webapps

RUN ./docker-config/scripts/build_couchdb_lucene.sh

RUN ./docker-config/scripts/download_liferay_and_dependencies.sh


FROM ubuntu:16.04

WORKDIR /app/

USER root

COPY ./scripts/install-thrift.sh .

COPY --from=builder /app/build/sw360/liferay-portal-7.2.0-ga1 /app/liferay-portal-7.2.0-ga1

COPY --from=builder /app/build/sw360/deployables/webapps /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/webapps

COPY --from=builder /app/build/sw360/deployables/deploy /app/liferay-portal-7.2.0-ga1/deploy

COPY ./docker-config/portal-ext.properties /app/liferay-portal-7.2.0-ga1

COPY ./docker-config/etc_sw360 /etc/sw360/

COPY ./docker-config/scripts .

COPY ./frontend/configuration/setenv.sh /app/liferay-portal-7.2.0-ga1/tomcat-9.0.17/bin

RUN ./install-thrift.sh

RUN ./install_init_postgres_script.sh

RUN ./install_configure_couchdb.sh

RUN apt-get install openjdk-8-jdk -y --no-install-recommends

ENTRYPOINT ./entry_point.sh && bash

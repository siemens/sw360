#!/usr/bin/env bash
# SPDX-FileCopyrightText: © 2024 Siemens AG
# SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
# SPDX-License-Identifier: EPL-2.0

set -o errexit -o nounset -o xtrace

docker compose up -d

#### Sleep for Tomcat to start
sleep 60

readonly HOST="127.0.0.1:8080"

#### Check health endpoint
curl --location -vv "http://${HOST}/resource/api/health"
curl --silent --location "http://${HOST}/resource/api/health" | grep -q '"status":"UP"'
curl --silent --location "http://${HOST}/resource/api/health" | grep -q '"isDbReachable":true'
curl --silent --location "http://${HOST}/resource/api/health" | grep -q '"isThriftReachable":true'

docker compose down

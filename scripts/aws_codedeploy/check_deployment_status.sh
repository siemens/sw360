#!/usr/bin/env bash

# -----------------------------------------------------------------------------
#
# Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Check Deployment Status
#
# -----------------------------------------------------------------------------

check_deployment_status() {
    i=1
    echo "Deployment id = $1"
    while [ $i -le 10 ]
    do
       sleep 1m
       deployment_status_info=$(aws deploy get-deployment --deployment-id $1)
       status=$(echo $deployment_status_info | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["deploymentInfo"]["status"]')
       if [[ ${status} == "Succeeded" ]]; then
          echo "Deployment successful. Waiting for app to start"
          sleep 5m;
          break;
       elif [[ ${status} == "Failed" ]]; then
          echo "Deployment failed"
          exit 1;
       fi
       ((i++))
    done
}

check_deployment_status $@


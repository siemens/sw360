#!/usr/bin/env bash

# -----------------------------------------------------------------------------
#
# Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Check is user allowed to trigger job
#
# -----------------------------------------------------------------------------

check_is_user_allowed_to_trigger_job() {
    if [[ ! ${ALLOWED_USER_LIST_TO_TRIGGER_DEPLOYMENT} =~ (.*;|^)${GITLAB_USER_EMAIL}(;.*|$) ]]; then
        echo "Operation not allowed for user - ${GITLAB_USER_EMAIL}."
        exit 403;
    fi
}

check_is_user_allowed_to_trigger_job


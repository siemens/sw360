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
# Upload packages to s3 from masterct
#
# -----------------------------------------------------------------------------

upload_to_s3() {
    if [[ "${CI_COMMIT_REF_SLUG}" == "${DEPLOY_BRANCH}" ]]; then
        echo "Uploading packages."
        aws s3 cp "${GITLAB_PACKAGE_PATH}/artifacts.zip" "s3://${SW360_BUCKET_NAME}/${PACKAGE_NAME_PREFIX}_${CI_COMMIT_REF_SLUG}_${CI_COMMIT_SHORT_SHA}.zip"
    fi
}

upload_to_s3


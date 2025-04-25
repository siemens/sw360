#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
# Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# This script replace Ternary fields "GPLv2Compat" and "GPLv3Compat" by Quadratic fields "OSIApproved" and "FSFLibre" in Licenses
# -----------------------------------------------------------------------------

import json
import time

from ibm_cloud_sdk_core.authenticators import BasicAuthenticator
from ibmcloudant.cloudant_v1 import CloudantV1

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

authenticator = BasicAuthenticator(username='user', password='pass')
client = CloudantV1(authenticator=authenticator)
client.set_service_url(COUCHSERVER)
client.configure_service(COUCHSERVER)

# ----------------------------------------
# queries
# ----------------------------------------

# get all licenses
all_licenses = {"selector": {"type": {"$eq": "license"}}, "limit": 20000}
# get moderations related to licenses
license_moderation_with_downloadurl_query = {"selector": {"type": {"$eq": "moderation"},"$or": [{"licenseAdditions": {"$exists": True}}, {"licenseDeletions": {"$exists": True}}]},"limit": 20000}

# ---------------------------------------
# functions
# ---------------------------------------

def replaceFields(license):
    for field in ['OSIApproved','FSFLibre']:
        if field not in license:
            license[field] = 'NA'
    for field in ['GPLv2Compat','GPLv3Compat']:
        if field in license:
            license.pop(field, None)
    return license

def replaceFieldsInLicenseModeration(moderation):
    for field in ['licenseAdditions','licenseDeletions']:
        moderation[field] = list(map(replaceFields, moderation[field]))
    return moderation

def run():
    log = {}
    # migrate licenses
    if DRY_RUN:
        print('------------------------------------------')
        print('Running in DRY mode')
        print('------------------------------------------')
        print('\n')
    print('On raw licenses: replace GPL compatibility fields by OSI approved and FSF libre fields.')
    licenses = client.post_find(
        db=DBNAME,
        selector=all_licenses["selector"],
        limit=all_licenses["limit"]
    ).get_result().get('docs', [])
    licenses_len = len(licenses)
    print(('Found ' + str(licenses_len) + ' licenses in db!'))

    license_log = {}
    license_log['totalCount'] = licenses_len
    license_log['replaceFieldsList'] = []
    for license in licenses:
        if not DRY_RUN:
            client.post_document(DBNAME, replaceFields(license)).get_result()
            replaceFieldsList = {}
            replaceFieldsList['id'] = license.get('_id')
            license_log['replaceFieldsList'].append(replaceFieldsList)

    # migrate moderations related to licenses
    print('In license moderations: replace GPL compatibility fields by OSI approved and FSF libre fields.')
    moderations_with_license_stuff = client.post_find(
        db=DBNAME,
        selector=license_moderation_with_downloadurl_query["selector"],
        limit=license_moderation_with_downloadurl_query["limit"]
    ).get_result().get('docs', [])
    moderations_len = len(moderations_with_license_stuff)
    print(('Found ' + str(moderations_len) + ' license moderations in db!'))

    moderation_log = {}
    moderation_log['totalCount'] = moderations_len
    moderation_log['replaceFieldsInLicenseModerationList'] = []
    for moderation in moderations_with_license_stuff:
        if not DRY_RUN:
            client.post_document(DBNAME, replaceFieldsInLicenseModeration(moderation)).get_result()
            replaceFieldsInLicenseModerationList = {}
            replaceFieldsInLicenseModerationList['id'] = moderation.get('_id')
            log['replaceFieldsInLicenseModerationList'].append(replaceFieldsInLicenseModerationList)

    log['licenses'] = license_log
    log['moderations'] = moderation_log
    logFile = open('044_replace_gpl_compatibility_by_osi_approved.log', 'w')
    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print('\n')
    print('------------------------------------------')
    print('Please check log file "044_replace_gpl_compatibility_by_osi_approved.log" in this directory for details')
    print('------------------------------------------')

# --------------------------------


startTime = time.time()
run()
print(('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'))

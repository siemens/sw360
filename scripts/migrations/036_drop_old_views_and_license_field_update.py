#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
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
# example, server address and db name should be parameterized, the code
# reorganized into a single class or function, etc.
#
# This script removes old design document for Risk & RiskCategory & merge "riskDatabaseIds"
# with "obligationDatabaseIds" and  remove "riskDatabaseIds" from license
# ----------------------------------------------------------------------------------------------

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

design_doc_id_risk = "Risk"
design_doc_id_riskcategory = "RiskCategory"


# ----------------------------------------
# queries
# ----------------------------------------

# get all license with "riskDatabaseIds"
all_license_with_riskDatabaseIds = {"selector": {"type": {"$eq": "license"}, "riskDatabaseIds": {"$exists": True}}, "limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------

def mergeRiskDBIdsWithObligationDBIds(resultFile):
    log = {}
    print('Getting all licenses with field riskDatabaseIds')
    all_licenses = client.post_find(
        db=DBNAME,
        selector=all_license_with_riskDatabaseIds["selector"],
        limit=all_license_with_riskDatabaseIds["limit"]
    ).get_result().get('docs', [])
    print('found ' + str(len(all_licenses)) + ' licenses with field riskDatabaseIds in db!')
    log['totalCount'] = len(all_licenses)
    log['updatedLicenseWithMergingRiskIdsOblIds'] = []

    for license in all_licenses:
        riskDBIds = license.get("riskDatabaseIds")
        oblDBIds = license.get("obligationDatabaseIds")
        if riskDBIds is not None:
            if oblDBIds is not None:
                oblDBIds = riskDBIds+oblDBIds
                license["obligationDatabaseIds"] = oblDBIds
            del license["riskDatabaseIds"]
        updatedLicense = {}
        updatedLicense['id'] = license.get('_id')
        log['updatedLicenseWithMergingRiskIdsOblIds'].append(updatedLicense)
        if not DRY_RUN:
            client.post_document(DBNAME, license).get_result()

    json.dump(log, resultFile, indent = 4)


def dropViews(doc_id, resultFile):
    log = {}
    log['docId'] = doc_id
    print('Getting Document by ID : ' + doc_id)
    doc = client.get_design_document(DBNAME, doc_id).get_result().get("_id", None)
    if doc is not None:
        print('Received document.Deleting Document.')
        print('Deleting Document with ID : ' + doc_id)
        log['result'] = 'Deleted Document with ID : ' + doc_id
        if not DRY_RUN:
            client.delete_design_document(doc_id).get_result()
    else:
        print('No document found with this ID.')
        log['result'] = 'No document found with this ID.'

    json.dump(log, resultFile, indent = 4)

def run():
    logFile = open('036_drop_old_views_and_license_field_update.log', 'w')
    mergeRiskDBIdsWithObligationDBIds(logFile)
    dropViews(design_doc_id_risk, logFile)
    dropViews(design_doc_id_riskcategory, logFile)
    logFile.close()



    print('\n')
    print('------------------------------------------')
    print('Please check log file "036_drop_old_views_and_license_field_update.log" in this directory for details')
    print('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

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
# This script renames the field "downloadurl" to "sourceCodeDownloadurl" in Release
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

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

# set fieldName
oldFieldName = "downloadurl"
newFieldName = "sourceCodeDownloadurl"

# ----------------------------------------
# queries
# ----------------------------------------

# get all releases with field "downloadurl"
releases_with_downloadurl_query = {"selector": {"type": {"$eq": "release"},oldFieldName: {"$exists": True}}, "limit": 200000}

# ---------------------------------------
# functions
# ---------------------------------------

def updateFieldNames(qryResult, oldName, newName, log):
    print('updating field name from '+oldName+' to '+newName)
    log['updated Release fields from '+oldName+' to '+newName] = []
    for entity in qryResult:
        entity[''+newName+''] = entity[''+oldName+'']
        del entity[''+oldName+'']
        if not DRY_RUN:
            client.post_document(DBNAME, entity).get_result()
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['updated Release fields from '+oldName+' to '+newName].append(updatedDocId)
    print('updation of field name from '+oldName+' to '+newName+' done')

def run():
    log = {}
    print('Getting all Release with field downloadurl')
    releases_with_downloadurl = client.post_find(
        db=DBNAME,
        selector=releases_with_downloadurl_query["selector"],
        limit=releases_with_downloadurl_query["limit"]
    ).get_result().get('docs', [])
    print('found ' + str(len(releases_with_downloadurl)) + ' Release with field downloadurl in db!')
    log['totalCount'] = len(releases_with_downloadurl)
    updateFieldNames(releases_with_downloadurl, oldFieldName, newFieldName, log)

    resultFile = open('040_release_migration_'+oldFieldName+'.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print('\n')
    print('------------------------------------------')
    print('Please check log file "040_release_migration_'+oldFieldName+'.log" in this directory for details')
    print('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

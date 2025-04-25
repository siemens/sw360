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
# This script removes the "obligationDatabaseIds" from obligation
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
deleteFieldName = "obligationDatabaseIds"

# ----------------------------------------
# queries
# ----------------------------------------

# get all obligation with field "obligationDatabaseIds"
obligation_with_obligationDatabaseIds_query = {"selector": {"type": {"$eq": "obligation"},deleteFieldName: {"$exists": True}}, "limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------

def removeFieldName(qryResult, fieldToBeRemoved, log):
    print('Removing field name '+fieldToBeRemoved+' starts')
    log['Updated obligation fields '+fieldToBeRemoved] = []
    for entity in qryResult:
        del entity[''+fieldToBeRemoved+'']
        if not DRY_RUN:
            client.post_document(DBNAME, entity).get_result()
            print('Removing field name '+fieldToBeRemoved+' done for '+entity.get('_id'))
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['Updated obligation fields '+fieldToBeRemoved].append(updatedDocId)

def run():
    log = {}
    print('Getting all obligation with field obligationDatabaseIds')
    obligation_with_obligationDatabaseIds = client.post_find(
        db=DBNAME,
        selector=obligation_with_obligationDatabaseIds_query["selector"],
        limit=obligation_with_obligationDatabaseIds_query["limit"]
    ).get_result().get('docs', [])
    print('found ' + str(len(obligation_with_obligationDatabaseIds)) + ' obligation with field obligationDatabaseIds in db!')
    log['totalCount'] = len(obligation_with_obligationDatabaseIds)
    removeFieldName(obligation_with_obligationDatabaseIds, deleteFieldName, log)


    resultFile = open('030_obligation__remove_field_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print('\n')
    print('------------------------------------------')
    print('Please check log file "030_obligation__remove_field_migration.log" in this directory for details')
    print('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

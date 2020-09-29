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
# This script removes "riskId" and "riskCategoryDatabaseId" from risk and adds the obligationType as 'RISK' and updates the type to obligation 
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = False

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# set fieldName
newValue = "obligation"

# ----------------------------------------
# queries
# ----------------------------------------

# get all risk
all_risk_query = {"selector": {"type": {"$eq": "risk"}}}

# ---------------------------------------
# functions
# ---------------------------------------

def updateDocType(log, logFile, qryResult):
    log['updatedObligation'] = []

    for risk in qryResult:
        print '\tUpdating type of document from risk to '+newValue+' for ID -> ' + risk.get('_id')
        risk['obligationType'] = 'RISK'
        risk['type'] = newValue
        updatedObligation = {}
        updatedObligation['id'] = risk.get('_id')
        log['updatedObligation'].append(updatedObligation)
        if not DRY_RUN:
            db.save(risk)
            print '\tUpdated type of document from risk to '+newValue+' for ID -> ' + risk.get('_id')
    
    json.dump(log, logFile, indent = 4, sort_keys = True)

def removeFieldName(log, logFile, qryResult, fieldToBeRemoved):
    print 'Removing field name '+fieldToBeRemoved
    log['Updated risk fields '+fieldToBeRemoved] = []
    for entity in qryResult:
        del entity[''+fieldToBeRemoved+'']
        if not DRY_RUN:
            db.save(entity)
            print 'Removing field name '+fieldToBeRemoved+' done for '+entity.get('_id')
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['Updated risk fields '+fieldToBeRemoved].append(updatedDocId)
    
    json.dump(log, logFile, indent = 4, sort_keys = True)


def run():
    log = {}
    logFile = open('risk_type_migration.log', 'w')

    print 'Getting all risk'
    all_risk = db.find(all_risk_query)
    print 'found ' + str(len(all_risk)) + ' risks in db!'
    log['totalCount'] = len(all_risk)

    removeFieldName(log, logFile, all_risk, "riskId");
    removeFieldName(log, logFile, all_risk, "riskCategoryDatabaseId");
    updateDocType(log, logFile, all_risk);

    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "risk_type_migration.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

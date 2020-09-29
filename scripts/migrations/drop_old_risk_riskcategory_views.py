#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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
# This script removes old design document for Risk & RiskCategory
# -----------------------------------------------------------------------------

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

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

design_doc_id_risk = "_design/Risk"
design_doc_id_riskcategory = "_design/RiskCategory"


# ----------------------------------------
# queries
# ----------------------------------------

# get all license with "riskDatabaseIds"
all_license_with_riskDatabaseIds = {"selector": {"type": {"$eq": "license"}, "riskDatabaseIds": {"$exists": True}}}

# ---------------------------------------
# functions
# ---------------------------------------

def mergeRiskDBIdsWithObligationDBIds(log, resultFile):
    print 'Getting all licenses'
    all_licenses = db.find(all_license_with_riskDatabaseIds)
    print 'found ' + str(len(all_licenses)) + ' licenses in db!'
    log['totalCount'] = len(all_licenses)

    for license in all_licenses:
        riskDBIds = license.get("riskDatabaseIds")
        print riskDBIds
        oblDBIds = license.get("obligationDatabaseIds")
        print oblDBIds
        if riskDBIds is not None:
            if oblDBIds is not None:
                oblDBIds = riskDBIds+oblDBIds
                print oblDBIds
                license["obligationDatabaseIds"] = oblDBIds
            del license["riskDatabaseIds"]
        print license
        if not DRY_RUN:
            db.save(license)

            
            

def dropViews(log, doc_id, resultFile):
    log['docId'] = doc_id
    print 'Getting Document by ID : ' + doc_id
    doc = db.get(doc_id, None)
    if doc is not None:
        print 'Received document.Deleting Document.'
        print 'Deleting Document with ID : ' + doc_id
        log['result'] = 'Deleted Document with ID : ' + doc_id
        if not DRY_RUN:
            db.delete(doc)
    else:
        print 'No document found with this ID.'
        log['result'] = 'No document found with this ID.'
    
    json.dump(log, resultFile, indent = 4)

def run():
    log = {}
    logFile = open('drop_old_risk_riskcategoriry_views.log', 'w')
    mergeRiskDBIdsWithObligationDBIds(log, logFile)
    #dropViews(log, design_doc_id_risk, logFile);
    #dropViews(log, design_doc_id_riskcategory, logFile);
    logFile.close()
    
    

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "drop_old_risk_riskcategoriry_views.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

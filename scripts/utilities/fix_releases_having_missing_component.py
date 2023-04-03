#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2022. Part of the SW360 Portal Project.
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
# This script is to fix corrupted releases.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# find missing component
get_all_releases = {"selector": {"type": {"$eq": "release"}}, "limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    logFile = open('fix_releases_having_missing_component.log', 'w')
    print "Getting all the releases"
    all_releases = db.find(get_all_releases);
    print 'found ' + str(len(all_releases)) + ' releases'
    componentIds = set()
    releaseIds = set()
    log = {}
    log['Data'] = []

    for rel in all_releases:
        componentId = rel.get("componentId")
        releaseId = rel.get("_id")
        temp = db.get(componentId);
        if temp is None:
            componentIds.add(componentId)
            releaseIds.add(releaseId)
            component_with_corrupted_rel_id = {"selector": { "type": { "$eq": "component" }, "releaseIds": { "$exists": True, "$elemMatch": { "$eq": ""+releaseId+"" } } }, "fields": ["_id","releaseIds"], "limit": 99999}
            correct_component_having_rel = db.find(component_with_corrupted_rel_id);
            for comp in correct_component_having_rel:
                correct_component_id = comp.get("_id")
                rel["componentId"] = correct_component_id
                if not DRY_RUN:
                    db.save(rel)
                msg = 'Corrupted release Id--> '+releaseId+' with missing component id--> '+componentId+ 'replaced with correct component with id--> '+correct_component_id
                log['Data'].append(msg);
                
    log['total missing components'] = len(componentIds)
    log['total corrupted releases'] = len(releaseIds)
    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "fix_releases_having_missing_component.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

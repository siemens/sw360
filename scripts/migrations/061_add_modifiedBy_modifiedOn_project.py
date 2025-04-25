#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
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
# This script is to add modifiedOn and modifiedBy in project.
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

MODIFIED_BY= 'modifiedBy'
MODIFIED_ON= 'modifiedOn'


# ----------------------------------------
# queries
# ----------------------------------------

# get all projects
get_all_project = {"selector": {"type": { "$eq": "project" },"modifiedOn": { "$exists": False },"modifiedBy": { "$exists": False }},"limit": 99999}


# ---------------------------------------
# functions
# ---------------------------------------

def run():
    logFile = open('061_add_modifiedBy_modifiedOn_project.log', 'w')
    all_projects = client.post_find(
        db=DBNAME,
        selector=get_all_project["selector"],
        limit=get_all_project["limit"]
    ).get_result().get('docs', [])
    all_ = list(all_projects)
    print(('found ' + str(len(all_)) + ' projects'))
    log = {}
    log['Project Ids'] = []

    for prj in all_:
           log['Project Ids'].append({"id": prj['_id']})
           prj[MODIFIED_BY]=""
           prj[MODIFIED_ON] =""
           if not DRY_RUN:
               client.post_document(DBNAME, prj).get_result()
    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "061_add_modifiedBy_modifiedOn_project.log" in this directory for details')
    print ('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print(('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'))

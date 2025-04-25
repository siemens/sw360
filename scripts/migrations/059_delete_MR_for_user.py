#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens Healthineers, 2022. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is for removing Moderation requests of a particular user.
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

EMAIL = "user_email"
MODERATION_STATE = ""

# ----------------------------------------
# queries
# ----------------------------------------

# remove the Moderation Requests created by a particular user
all_Moderation_Requests = {
    "selector": {
        "type": {"$eq": "moderation"},
        "requestingUser": {"$eq": EMAIL},
    },
    "limit": 99999
}

if MODERATION_STATE:
    all_Moderation_Requests["selector"]["moderationState"] = {"$eq": MODERATION_STATE}

# ---------------------------------------
# functions
# ---------------------------------------
def removeModerationRequests(logFile, docs):
    log = {}
    log['total MR'] = len(list(docs))
    log['MR'] = []
    log['MR(Dry run)'] = []
    for mr in docs:
        print(("MR Id: " + mr.get("_id")))
        if DRY_RUN:
            deleteMR_Dry_Run = {}
            deleteMR_Dry_Run['MRid'] = mr.get('_id')
            log['MR(Dry run)'].append(deleteMR_Dry_Run)
        if not DRY_RUN:
            client.delete_document(DBNAME, mr.get('_id')).get_result()
            deleteMR = {}
            deleteMR['id'] = mr.get('_id')
            log['MR'].append(deleteMR)

    json.dump(log, logFile, indent = 4, sort_keys = True)


def run():
    logFile = open('RemoveMR.log', 'w')
    print ('Getting all the Moderation Requests')
    Moderation_Requests = list(client.post_find(
        db=DBNAME,
        selector=all_Moderation_Requests["selector"],
        limit=all_Moderation_Requests["limit"]
    ).get_result().get('docs', []))
    print(('found ' + str(len(Moderation_Requests)) + ' Moderation Requests\n'))
    removeModerationRequests(logFile, Moderation_Requests)
    logFile.close()
    print ('------------------------------------------')
    print ('Please check log file "RemoveMR.log" in this directory for details')
    print ('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print(('\nTime of deletion: ' + "{0:.2f}".format(time.time() - startTime) + 's'))

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
# This script is for removing deactivated users from moderators list.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import json
import time

from ibm_cloud_sdk_core.authenticators import BasicAuthenticator
from ibmcloudant.cloudant_v1 import CloudantV1

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = False

COUCHSERVER = 'http://localhost:5984/'
SW360_DB = 'sw360db'
USERS_DB = 'sw360users'

authenticator = BasicAuthenticator(username='user', password='pass')
client = CloudantV1(authenticator=authenticator)
client.set_service_url(COUCHSERVER)
client.configure_service(COUCHSERVER)

MODS = "moderators"
EMAIL = "email"
BOOL_TRUE = True
ID = "_id"
STATE = "moderationState"

# ----------------------------------------
# queries
# ----------------------------------------

# get all the deactivated users
inactive_users_query = {"selector": {"type": {"$eq": "user"},"deactivated": {"$eq": BOOL_TRUE}},"limit": 99999}

# get all the moderation requests
mr_query = {"selector": {"type": "moderation"}, "limit": 999999}


# ---------------------------------------
# functions
# ---------------------------------------

#Remove_InactiveUsers_From_Moderators
def activeUsersInModerators(log, inactive_users, moderation_list):
    log["Inactive Users email id in Moderators with moderation id"] =[]
    user_list = list(inactive_users)

    for moderation in moderation_list:
        for user in user_list:
            for iteration, item in enumerate(moderation[MODS]):
                if (item == user[EMAIL]):
                    moderation[MODS].remove(item)
                    print((moderation[ID]))
                    print((moderation[STATE]))
                    log['Inactive Users email id in Moderators with moderation id'].append(moderation[ID])
                    log['Inactive Users email id in Moderators with moderation id'].append(item)

        if not DRY_RUN:
            client.post_document(SW360_DB, moderation).get_result()

def run():
    log = {}
    logFile = open('054_moderationReq.log', 'w')

    print ('Updated Users detail in log file:')
    print ('\n')
    inactive_users = client.post_find(
        db=USERS_DB,
        selector=inactive_users_query["selector"],
        limit=inactive_users_query["limit"]
    ).get_result().get('docs', [])
    mod_requests = client.post_find(
        db=SW360_DB,
        selector=mr_query["selector"],
        limit=mr_query["limit"]
    ).get_result().get('docs', [])
    moderation_list = list(mod_requests)

    activeUsersInModerators(log, inactive_users, moderation_list)
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "054_moderationReq.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print(('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'))

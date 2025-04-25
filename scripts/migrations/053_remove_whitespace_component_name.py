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
# This script is for removing the trailing and leading whitespaces in component's name
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import json
import time

import pandas as pd
from ibm_cloud_sdk_core.authenticators import BasicAuthenticator
from ibmcloudant.cloudant_v1 import CloudantV1

#Constants
DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

def dbConnection():
    print('Connecting to local couchdb stage server....../')
    authenticator = BasicAuthenticator(username='user', password='pass')
    client = CloudantV1(authenticator=authenticator)
    client.set_service_url(COUCHSERVER)
    client.configure_service(COUCHSERVER)
    return client

def queryExecution(client, type):
    print(f'Executing the {type} whitespace query.................../')
    db_query = client.post_find(
        db=DBNAME,
        selector={"type": type,"name": {"$regex": "(^\\s.+)|(.+\\s$)"}},
        limit=999999999999999
    ).get_result().get('docs', [])
    if bool(db_query):
        return list(db_query)
    else:
        return []

def updateDB(client, db_copy_list):
    print('Correcting the component/release names and updating the database......../')

    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()
    df = df.fillna('')
    db_df_list = df.to_dict('records')
    if not DRY_RUN:
        #Update call
        client.post_document(DBNAME, db_df_list).get_result()

    return db_df_list

def generateLogFile(header, db_copy_list):
    print('Generating log file......../')
    with open("_053_remove_whitespace_component_name.log", "a") as f:
        f.write(header)
    keys_to_extract = ['_id', 'name']
    result = [{key: d[key] for key in keys_to_extract if key in d} for d in db_copy_list]
    logFile = open('_053_remove_whitespace_component_name.log', 'a')
    json.dump(result, logFile, indent = 4, sort_keys = True)
    logFile.close()

def checkDuplicate(client, db_copy_list):
    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()
    df = df.fillna('')
    results = client.post_find(
        db=DBNAME,
        selector={"type": {"$eq": "component"},"$or": [{"name": name} for name in df['name']]},
        limit=99999
    ).get_result().get('docs', [])
    return list(results)

def checkDuplicateReleases(client, db_copy_list):
    print('Checking duplicate releases......./')
    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()
    df = df.fillna('')
    existing_release_list = client.post_find(
        db=DBNAME,
        selector={"type": {"$eq": "release"},"$or": [{"name": name} for name in df['name']]},
        limit=99999
    ).get_result().get('docs', [])
    db_copy_list = df.to_dict('records')
    dup_rel_list = []
    for rel in list(existing_release_list):
        for x in db_copy_list:
            if(rel['name']==x['name'] and rel['componentId']==x['componentId'] and rel['version']==x['version']):
                dup_rel_list.append(x)
    return dup_rel_list

def linkReleaseToComponent(client, db_corrected_comp_list, db_existing_comp_list):

    print('######################')
    print('Linking Releases to Components........./')

    db_duplicate_corrected_comp_list = [x for x in db_corrected_comp_list if x['name'] in [d['name'] for d in db_existing_comp_list]]

    db_upd_comp_list = []
    db_upd_releases_list = []
    db_del_comp_Ids = []
    for x in list(db_duplicate_corrected_comp_list):
        for comp in db_existing_comp_list:
            if(x['name'] == comp['name']):
                db_del_comp_Ids.append(x['_id'])
                for i in x["releaseIds"]:
                    relation = client.post_find(
                        db=DBNAME,
                        selector={"type": {"$eq": "release"}, "_id": { "$eq": i }},
                        limit=99999
                    ).get_result().get('docs', [])
                    relation_list = list(relation)
                    print((relation_list[0]["componentId"]))
                    relation_list[0]["componentId"] = comp['_id']
                    print((relation_list[0]["componentId"]))
                    if("releaseIds" not in comp):
                        comp["releaseIds"] = []
                    comp["releaseIds"].append(i)
                    db_upd_releases_list.append(relation_list[0])
                    db_upd_comp_list.append(comp)

    db_dup_release_list = checkDuplicateReleases(client, db_upd_releases_list)

    #Generate log file
    header = (f'Duplicate releases that will be merged - total count :  {len(db_dup_release_list)}')
    generateLogFile(header, db_dup_release_list)

    #Generate log file
    header = (f'Releases that will be merged - total count :  {len(db_upd_releases_list)}')
    generateLogFile(header, db_upd_releases_list)

    print('Updating Linkage of Components and Releases............/')
    if not DRY_RUN:
        client.post_document(DBNAME, db_upd_comp_list).get_result()
        client.post_document(DBNAME, db_upd_releases_list).get_result()

    print(db_del_comp_Ids)
    return db_del_comp_Ids

def deleteDuplicateComponents(client, db_del_comp_Ids):
    if not DRY_RUN:
        #Generate log file
        header = (f'Duplicate components that are getting deleted {len(db_del_comp_Ids)}')
        generateLogFile(header, db_del_comp_Ids)
        print('Deleting duplicate components........../')
        for doc_id in db_del_comp_Ids :
            client.delete_document(DBNAME, doc_id).get_result()

def run():

    #DB connection
    client = dbConnection()

    #Query to fetch the component names with whitespace
    db_comp_list = queryExecution(client,'component')
    #Generate log file
    header = (f'Components with whitespaces - total count :  {len(db_comp_list)}')
    generateLogFile(header, db_comp_list)

    #Query to fetch the release names with whitespaces
    db_rel_list = queryExecution(client,'release')
    #Generate log file
    header = (f'Releases with whitespaces - total count :  {len(db_rel_list)}')
    generateLogFile(header, db_rel_list)

    #check Duplicate
    if len(db_comp_list)!=0:
        db_existing_comp_list = checkDuplicate(client,db_comp_list)
        #Generate log file
        header = (f'Existing components with the same name - total count :  {len(db_existing_comp_list)}')
        generateLogFile(header, db_existing_comp_list)

    #Converting document data to dataframe and removing the whitespace of components
    if len(db_comp_list)!=0:
        db_corrected_comp_list = updateDB(client,db_comp_list)
        #Generate log file
        header = (f'Corrected components - total count :  {len(db_corrected_comp_list)}')
        generateLogFile(header, db_corrected_comp_list)
    else:
        print('No Records found!')

    #Converting document data to dataframe and removing the whitespace of releases
    if len(db_rel_list)!=0:
        db_corrected_rel_list = updateDB(client,db_rel_list)
        #Generate log file
        header = (f'Corrected components - total count :  {len(db_corrected_rel_list)}')
        generateLogFile(header, db_corrected_rel_list)
    else:
        print('No Records found!')

    #link Releases of duplicate components to the existing components
    if len(db_corrected_comp_list)!=0:
        db_del_comp_Ids = linkReleaseToComponent(client, db_corrected_comp_list, db_existing_comp_list)

    #delete duplicate components
    if len(db_del_comp_Ids)!=0:
        deleteDuplicateComponents(client, db_del_comp_Ids)

    print('Execution completed....')

if __name__ == '__main__':
    try:
        start_time = time.time()
        run()
        print(('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's'))

    except Exception as e:
        print(('Exception message ',e))

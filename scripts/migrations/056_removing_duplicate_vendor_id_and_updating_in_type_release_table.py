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
# -----------------------------------------------------------------------------

from logging import exception
import couchdb
import copy
import pandas as pd
import json
import math
import re



DRY_RUN = False

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

# set admin name and password for couchdb3
DB_USER_NAME = 'admin'
DB_USER_PASSWORD = 'sw360fossy12345'


def dbConnection():
    print('Connecting to local couchdb stage server....../')
    couch = couchdb.Server(COUCHSERVER)
    couch.resource.credentials=(DB_USER_NAME, DB_USER_PASSWORD)
    return couch[DBNAME]

# Load the Excel file
df= pd.read_excel('/home/nikesh/Desktop/Manufacturers1.xlsx',header=0,names= ['FullName','ShortName','URL','SW360ID', 'Action'], engine='openpyxl')


def queryExecution(db):
    print('Executing the query.................../')
    db_query = db.find({"selector":{"type": "vendor","fullname": {"$regex": "(^\\s.+)|(.+\\s$)"},"url": {"$regex": "(^\\s.+)|(.+\\s$)"},"shortname": {"$regex": "(^\\s.+)|(.+\\s$)"},"limit":999}})
    if bool(db_query):
        return list(db_query)
    else:
        return []

def create_json_file():
      matches = []
      dictionary_name={}
      df = df[~df['Action'].str.startswith("delete",na=False)]
      for i, row in df.iterrows():
            if not pd.isnull(row['Action']):
                  temp = {row['SW360ID'] : row['Action']}
                  
                  dictionary_name.update(temp)
      with open('temp_json.json', 'w') as file:
            json.dump(dictionary_name, file) 

# Extract data from database
def get_data_based_on_sw360_id(action_id):

      db = dbConnection()
      action_id_list = []
      action_id_list.append(action_id)
      print('data fetching started !')
      # db_query = '''function(doc) { if (doc.type == 'vendor'){ emit(doc.vendorId, doc._id);  }}'''
      db_query = {
                  "selector": {
                  "_id": {
                  "$eq": action_id_list[0]
                              }
                        }
                  }
      data = db.find(db_query)
      print(type(data))
      data = list(data)
      print(data)
      # print(data._id, data._rev, data.type, data.shortname, data.fullname, data.url )
def update_data_based_on_sw360_id(sw360_id,action_id):
      db = dbConnection()
      #get_data_based_on_sw360_id(action_id)
      sw360_id = action_id
      print(sw360_id)
      sw360_id_list = []
      sw360_id_list.append(sw360_id)
      db_query = {
                  "selector": {
                  "_id": {
                  "$eq": sw360_id_list[0]
                              }
                        }
                  }
      print(db_query)
      db.save(db_query)


# Get the ids from mapping json file
def extract_id_from_mapping_json():
      
      with open('temp_json.json', 'r') as file:
            id_mapping_file = json.load(file)
      for sw360_id, action in id_mapping_file.items():
            if re.search(r'use (\w+); delete', action):
                  action_id = re.findall(r'use (\w+); delete', action)[0]
                  print(action_id)
                  get_data_based_on_sw360_id(action_id)
                  update_data_based_on_sw360_id(sw360_id,action_id)
                  break
            else:
                  continue
            
      
      
      print('Id retirieved !')



def run():
 
    #DB connection
    db = dbConnection()
    extract_id_from_mapping_json()

       


def generateLogFile(db_copy_list):
    print('Generating log file......../')
    logFile = open('vendor_name.log', 'w')
    json.dump(db_copy_list, logFile, indent = 4, sort_keys = True)
    logFile.close()

    
    
 
run()

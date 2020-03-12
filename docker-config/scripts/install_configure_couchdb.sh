#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script installs couchdb, configures couchdb-lucene
# and accessibility from host machine.
# -----------------------------------------------------------------------------

install_configure_couchdb() {
  install_couchdb
  configure_couchdb_db
}

install_couchdb() {
  apt-get install curl -y --no-install-recommends
  curl -L https://couchdb.apache.org/repo/bintray-pubkey.asc  | apt-key add
  apt-get update
  apt-get install couchdb -y --no-install-recommends
}

configure_couchdb_db() {
  sed -i "s/\[httpd_global_handlers\]/\[httpd_global_handlers\]\n_fti = {couch_httpd_proxy, handle_proxy_req, <<\"http:\/\/localhost:8080\/couchdb-lucene\">>}/" /etc/couchdb/local.ini
  sed -i "s/bind_address = 127.0.0.1/bind_address = 0.0.0.0/" /etc/couchdb/default.ini
}

install_configure_couchdb

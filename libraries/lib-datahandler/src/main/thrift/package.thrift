/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.packages
namespace php sw360.thrift.packages

typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.RequestSummary RequestSummary
typedef sw360.SW360Exception SW360Exception
typedef users.User User

struct Package {
    1: optional string id,
    2: optional string revision,
    3: required string type = "package",
    4: required string name,
    5: required string version,
    6: optional string vendor,
    7: required string releaseId,
    8: optional list<string> declaredLicenses,
    9: optional string description,
    10: optional string homepageUrl,
    11: optional string binaryArtifactUrl,
    12: optional string sourceArtifactUrl,
    13: optional string vcs,
    14: optional string vcsProcessed,
    15: optional string createdOn,
    16: optional string createdBy,
    17: optional PackageManagerType packageManagerType
}

enum PackageManagerType {
    NUGET = 0,
    NPM = 1,
    PIP = 2,
    PIP_ENV = 3,
    DOT_NET = 4,
    MAVEN = 5,
    GRADLE = 6,
    COMPOSER = 7,
    SBT = 8
}

service PackageService {
    /**
     * get Package by Id
     */
    Package getPackageById(1: string packageId);

    /**
     * get Packages by list of Id
     */
    list<Package> getPackageByIds(1: set<string> ids);

    /**
     * get All Packages for landing page
     */
    list<Package> getAllPackages();

    /**
     * get Packages by list for release Ids
     */
    set<Package> getPackagesByReleaseIds(1: set<string> ids);

     /**
      * add package to database with user as creator, return id
      **/
    AddDocumentRequestSummary addPackage(1: Package pkg, 2: User user);

    /**
     * parse a ORT analyzer file and write the information to SW360
     **/
    RequestSummary importPackagesFromAttachmentContent(1: User user, 2:string attachmentContentId);
}

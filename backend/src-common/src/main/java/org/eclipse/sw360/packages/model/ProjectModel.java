/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.packages.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectModel {

    private String id;

    @JsonProperty("declared_licenses")
    private List<String> declaredLicenses;

    @JsonProperty("vcs")
    private VcsModel vcs;

    @JsonProperty("vcs_processed")
    private VcsModel vcsProcessed;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the declaredLicenses
     */
    public List<String> getDeclaredLicenses() {
        return declaredLicenses;
    }

    /**
     * @param declaredLicenses the declaredLicenses to set
     */
    public void setDeclaredLicenses(List<String> declaredLicenses) {
        this.declaredLicenses = declaredLicenses;
    }

    /**
     * @return the vcs
     */
    public VcsModel getVcs() {
        return vcs;
    }

    /**
     * @param vcs the vcs to set
     */
    public void setVcs(VcsModel vcs) {
        this.vcs = vcs;
    }

    /**
     * @return the vcsProcessed
     */
    public VcsModel getVcsProcessed() {
        return vcsProcessed;
    }

    /**
     * @param vcsProcessed the vcsProcessed to set
     */
    public void setVcsProcessed(VcsModel vcsProcessed) {
        this.vcsProcessed = vcsProcessed;
    }

    /**
     * @return the homepageUrl
     */
    public String getHomepageUrl() {
        return homepageUrl;
    }

    /**
     * @param homepageUrl the homepageUrl to set
     */
    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }
}
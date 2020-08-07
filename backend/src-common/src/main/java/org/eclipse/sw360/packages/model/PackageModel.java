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
public class PackageModel {

    private String id;

    @JsonProperty("declared_licenses")
    private List<String> declaredLicenses;

    private String purl;

    private String description;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    @JsonProperty("binary_artifact")
    private ArtifactModel binaryArtifact;

    @JsonProperty("source_artifact")
    private ArtifactModel sourceArtifact;

    @JsonProperty("vcs")
    private VcsModel vcs;

    @JsonProperty("vcs_processed")
    private VcsModel vcsProcessed;

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
     * @return the purl
     */
    public String getPurl() {
        return purl;
    }

    /**
     * @param purl the purl to set
     */
    public void setPurl(String purl) {
        this.purl = purl;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
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

    /**
     * @return the binaryArtifact
     */
    public ArtifactModel getBinaryArtifact() {
        return binaryArtifact;
    }

    /**
     * @param binaryArtifact the binaryArtifact to set
     */
    public void setBinaryArtifact(ArtifactModel binaryArtifact) {
        this.binaryArtifact = binaryArtifact;
    }

    /**
     * @return the sourceArtifact
     */
    public ArtifactModel getSourceArtifact() {
        return sourceArtifact;
    }

    /**
     * @param sourceArtifact the sourceArtifact to set
     */
    public void setSourceArtifact(ArtifactModel sourceArtifact) {
        this.sourceArtifact = sourceArtifact;
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
}

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryModel {

    @JsonProperty("vcs")
    private VcsModel vcs;

    @JsonProperty("vcs_processed")
    private VcsModel vcsProcessed;

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
}

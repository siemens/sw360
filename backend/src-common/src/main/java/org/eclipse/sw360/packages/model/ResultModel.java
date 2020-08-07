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

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultModel {

    private List<ProjectModel> projects;

    private List<Packages> packages;
    /**
     * @return the projects
     */
    public List<ProjectModel> getProjects() {
        return projects;
    }

    /**
     * @param projects the projects to set
     */
    public void setProjects(List<ProjectModel> projects) {
        this.projects = projects;
    }

    /**
     * @return the packages
     */
    public List<Packages> getPackages() {
        return packages;
    }

    /**
     * @param packages the packages to set
     */
    public void setPackages(List<Packages> packages) {
        this.packages = packages;
    }
}


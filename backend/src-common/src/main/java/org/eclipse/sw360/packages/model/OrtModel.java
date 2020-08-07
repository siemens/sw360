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

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrtModel {
    private RepositoryModel repository;
    private AnalyzerModel analyzer;

    /**
     * @return the repository
     */
    public RepositoryModel getRepository() {
        return repository;
    }
    /**
     * @param repository the repository to set
     */
    public void setRepository(RepositoryModel repository) {
        this.repository = repository;
    }
    /**
     * @return the analyzer
     */
    public AnalyzerModel getAnalyzer() {
        return analyzer;
    }
    /**
     * @param analyzer the analyzer to set
     */
    public void setAnalyzer(AnalyzerModel analyzer) {
        this.analyzer = analyzer;
    }
}

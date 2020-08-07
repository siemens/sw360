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
public class AnalyzerModel {

    private ResultModel result;

    /**
     * @return the result
     */
    public ResultModel getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(ResultModel result) {
        this.result = result;
    }
}

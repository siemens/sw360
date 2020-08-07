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
public class Packages {

    @JsonProperty("package")
    private PackageModel pckg;

    /**
     * @return the pckg
     */
    public PackageModel getPckg() {
        return pckg;
    }

    /**
     * @param pckg the pckg to set
     */
    public void setPckg(PackageModel pckg) {
        this.pckg = pckg;
    }
}

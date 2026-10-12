/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ComponentModerationRequestGenerator extends ModerationRequestGenerator<Component._Fields, Component> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Component updateComponent, Component actualComponent){
        updateDocument = updateComponent;
        actualDocument = actualComponent;

        // Reuse the additions/deletions already accumulated on the request (if any),
        // instead of always starting from a blank Component. This ensures that when
        // multiple moderation requests are created for the same component while a
        // previous one is still pending, the diffs get combined/merged into the
        // existing request rather than the new diff replacing/overwriting it.
        // See ProjectModerationRequestGenerator for the equivalent, already working,
        // pattern used for Project (and transitively for Release) moderation requests.
        documentAdditions = request.isSetComponentAdditions() ? request.getComponentAdditions() : new Component();
        documentDeletions = request.isSetComponentDeletions() ? request.getComponentDeletions() : new Component();
        //required fields:
        documentAdditions.setName(updateComponent.getName());
        documentAdditions.setId(actualComponent.getId());
        documentDeletions.setName(actualComponent.getName());
        documentDeletions.setId(actualComponent.getId());

        for (Component._Fields field : Component._Fields.values()) {

            //ignored fields - must be skipped unconditionally, BEFORE the null-based
            //addition/deletion checks below. Otherwise, whenever the incoming "update"
            //component does not carry these (aggregated/derived) fields - which is the
            //normal case for a plain field edit coming from the UI/REST layer - they
            //would be wrongly treated as "deletions" of the actual component's values,
            //causing e.g. Releases to incorrectly show up as part of the moderation
            //request even though only an unrelated field was changed.
            switch (field) {
                case PERMISSIONS:
                case DOCUMENT_STATE:
                //releases and related fields are not updated via moderation requests
                case RELEASES:
                case RELEASE_IDS:
                case MAIN_LICENSE_IDS:
                case LANGUAGES:
                case OPERATING_SYSTEMS:
                case VENDOR_NAMES:
                    continue;
                default:
                    break;
            }

            if(actualComponent.getFieldValue(field) == null){
                documentAdditions.setFieldValue(field, updateComponent.getFieldValue(field));

            } else if (updateComponent.getFieldValue(field) == null){
                documentDeletions.setFieldValue(field, actualComponent.getFieldValue(field));

            } else if(!actualComponent.getFieldValue(field).equals(updateComponent.getFieldValue(field))) {
                switch (field) {
                    case ATTACHMENTS:
                        dealWithAttachments(Component._Fields.ATTACHMENTS);
                        break;
                    case ROLES:
                        dealWithCustomMap(Component._Fields.ROLES);
                        break;
                    case EXTERNAL_IDS:
                        dealWithStringtoStringMap(Component._Fields.EXTERNAL_IDS);
                        break;
                    case ADDITIONAL_DATA:
                        dealWithStringKeyedMap(Component._Fields.ADDITIONAL_DATA);
                        break;
                    default:
                        dealWithBaseTypes(field, Component.metaDataMap.get(field));
                }
            }
        }

        request.setComponentAdditions(documentAdditions);
        request.setComponentDeletions(documentDeletions);
        return request;
    }
}

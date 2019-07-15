/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.dcae.checker.Catalog;
import org.onap.sdc.dcae.checker.Checker;
import org.onap.sdc.dcae.checker.Construct;
import org.onap.sdc.dcae.checker.Facet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;

public class CapabilityCommon extends BaseCommon {

    private static CapabilityCommon instance;

    public synchronized static CapabilityCommon getInstance() {
        if (instance == null)
        {
            instance = new CapabilityCommon();
        }
        return instance;
    }

    private CapabilityCommon() {}



    public void check_capabilities(Map<String, Map> theDefinition,
                                    Checker.CheckContext theContext, Catalog catalog) {
        theContext.enter(CAPABILITIES);
        try {
            if (!CheckCommon.getInstance().checkDefinition(CAPABILITIES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkCapabilityDefinition(e.getKey(), e.getValue(), theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    /* A capability definition appears within the context ot a node type */
    public void checkCapabilityDefinition(String theName,
                                          Map theDef,
                                          Checker.CheckContext theContext,
                                          Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName, Construct.Capability);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }

            //check capability type
            if (!typeCommon.checkType(Construct.Capability, theDef, theContext, catalog)) {
                return;
            }

            //check properties
            if (!facetCommon.checkFacetAugmentation(
                    Construct.Capability, theDef, Facet.properties, theContext, catalog)) {
                return;
            }

            //check attributes
            if (!facetCommon.checkFacetAugmentation(
                    Construct.Capability, theDef, Facet.attributes, theContext, catalog)) {
                return;
            }

            //valid_source_types: should point to valid template nodes
            if (theDef.containsKey(VALID_SOURCE_TYPES)) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog,
                        ((List<String>) theDef.get(VALID_SOURCE_TYPES)).toArray(EMPTY_STRING_ARRAY));
                //per A.6.1.4 there is an additinal check to be performed here:
                //"Any Node Type (names) provides as values for the valid_source_types keyname SHALL be type-compatible (i.e., derived from the same parent Node Type) with any Node Types defined using the same keyname in the parent Capability Type."
            }
            //occurences: were verified in range_definition

        } finally {
            theContext.exit();
        }
    }

    public void checkCapabilityTypeDefinition(String theName,
                                              Map theDefinition,
                                              Checker.CheckContext theContext,
                                              Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        AttributesCommon attributesCommon = AttributesCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName, Construct.Capability);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Capability, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                attributesCommon.checkAttributes(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Capability, theName, theDefinition,
                        Facet.attributes, theContext, catalog);
            }

            //valid_source_types: see capability_type_definition
            //unclear: how is the valid_source_types list definition eveolving across
            //the type hierarchy: additive, overwriting, ??
            if (theDefinition.containsKey(VALID_SOURCE_TYPES)) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog,
                        ((List<String>) theDefinition.get(VALID_SOURCE_TYPES)).toArray(EMPTY_STRING_ARRAY));
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkCapabilitiesAssignmentDefinition(
            Map<String, Map> theCapabilities, Checker.CheckContext theContext, Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(CAPABILITIES);
        try {
            if (!CheckCommon.getInstance().checkDefinition(CAPABILITIES, theCapabilities, theContext)) {
                return;
            }

            //the node type for the node template enclosing these requirements
            String nodeType = (String) catalog.getTemplate(
                    theContext.target(),
                    Construct.Node,
                    theContext.enclosingConstruct(Construct.Node))
                    .get("type");

            for (Iterator<Map.Entry<String, Map>> ci =
                 theCapabilities.entrySet().iterator();
                 ci.hasNext(); ) {

                Map.Entry<String, Map> ce = ci.next();

                String capabilityName = ce.getKey();
                Map capabilityDef = facetCommon.findTypeFacetByName(Construct.Node, nodeType,
                        Facet.capabilities, capabilityName, catalog);
                if (capabilityDef == null) {
                    theContext.addError("No capability " + capabilityName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkCapabilityAssignmentDefinition(
                        capabilityName, ce.getValue(), capabilityDef, theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkCapabilityAssignmentDefinition(
            String theCapabilityName,
            Map theAssignment,
            Map theDefinition,
            Checker.CheckContext theContext,
            Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(theCapabilityName, Construct.Capability);
        try {
            String capabilityType = (String) theDefinition.get("type");
            //list of property and attributes assignments
            facetCommon.checkFacet(Construct.Capability, theAssignment, capabilityType,
                    Facet.properties, theContext, catalog);
            facetCommon.checkFacet(Construct.Capability, theAssignment, capabilityType,
                    Facet.attributes, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }
}

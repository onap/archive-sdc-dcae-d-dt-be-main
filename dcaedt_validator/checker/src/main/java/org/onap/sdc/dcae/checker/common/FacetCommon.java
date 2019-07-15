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

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.onap.sdc.dcae.checker.common.ConstCommon.DEFAULT;
import static org.onap.sdc.dcae.checker.common.ConstCommon.TYPE;
import static org.onap.sdc.dcae.checker.common.ConstCommon.UNKNOWN;

public class FacetCommon extends BaseCommon {

    private static FacetCommon instance;

    public synchronized static FacetCommon getInstance() {
        if (instance == null)
        {
            instance = new FacetCommon();
        }
        return instance;
    }

    private FacetCommon() {}
    /**
     * Given the type of a certain construct (node type for example), look up
     * in one of its facets (properties, capabilities, ..) for one of the given
     * facet type (if looking in property, one of the given data type).
     *
     * @return a map of all facets of the given type, will be empty to signal
     * none found
     * <p>
     * Should we look for a facet construct of a compatible type: any type derived
     * from the given facet's construct type??
     */
    public Map<String, Map>
    findTypeFacetByType(Construct theTypeConstruct,
                        String theTypeName,
                        Facet theFacet,
                        String theFacetType,
                        Catalog catalog) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType {}, {}: {} {}", theTypeName, theTypeConstruct, theFacetType, theFacet);
        Map<String, Map> res = new HashMap<>();
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theTypeConstruct, theTypeName);
        while (i.hasNext()) {
            Map.Entry<String, Map> typeSpec = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, Checking {} type {}", theTypeConstruct, typeSpec.getKey());
            Map<String, Map> typeFacet =
                    (Map<String, Map>) typeSpec.getValue().get(theFacet.name());
            if (typeFacet == null) {
                continue;
            }
            Iterator<Map.Entry<String, Map>> fi = typeFacet.entrySet().iterator();
            while (fi.hasNext()) {
                Map.Entry<String, Map> facet = fi.next();
                String facetType = (String) facet.getValue().get("type");
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, Checking {} type {}", facet.getKey(), facetType);

                //here is the question: do we look for an exact match or ..
                //now we check that the type has a capability of a type compatible
                //(equal or derived from) the given capability type.
                if (catalog.isDerivedFrom(
                        theFacet.construct(), facetType, theFacetType)) {
                    res.putIfAbsent(facet.getKey(), facet.getValue());
                }
            }
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, found {}", res);

        return res;
    }

    public Map<String, Object>
    findTypeFacetByName(Construct theTypeConstruct,
                        String theTypeName,
                        Facet theFacet,
                        String theFacetName,
                        Catalog catalog) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByName {} {}", theTypeConstruct, theTypeName);
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theTypeConstruct, theTypeName);
        while (i.hasNext()) {
            Map.Entry<String, Map> typeSpec = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByName, Checking {} type {}", theTypeConstruct, typeSpec.getKey());
            Map<String, Map> typeFacet =
                    (Map<String, Map>) typeSpec.getValue().get(theFacet.name());
            if (typeFacet == null) {
                continue;
            }
            Map<String, Object> facet = typeFacet.get(theFacetName);
            if (facet != null) {
                return facet;
            }
        }
        return null;
    }

    /* Check that a particular facet (properties, attributes) of a construct type
     * (node type, capability type, etc) is correctly (consistenly) defined
     * across a type hierarchy
     */
    public boolean checkTypeConstructFacet(Construct theConstruct,
                                           String theTypeName,
                                           Map theTypeSpec,
                                           Facet theFacet,
                                           Checker.CheckContext theContext,
                                           Catalog catalog) {
        Map<String, Map> defs =
                (Map<String, Map>) theTypeSpec.get(theFacet.name());
        if (null == defs) {
            return true;
        }

        boolean res = true;

        //given that the type was cataloged there will be at least one entry
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theConstruct, theTypeName);
        if (!i.hasNext()) {
            theContext.addError(
                    "The type " + theTypeName + " needs to be cataloged before attempting 'checkTypeConstruct'", null);
            return false;
        }
        i.next(); //skip self
        while (i.hasNext()) {
            Map.Entry<String, Map> e = i.next();
            Map<String, Map> superDefs = (Map<String, Map>) e.getValue()
                    .get(theFacet.name());
            if (null == superDefs) {
                continue;
            }
            //this computes entries that appear on both collections but with different values, i.e. the re-defined properties
            Map<String, MapDifference.ValueDifference<Map>> diff = Maps.difference(defs, superDefs).entriesDiffering();

            for (Iterator<Map.Entry<String, MapDifference.ValueDifference<Map>>> di = diff.entrySet().iterator(); di.hasNext(); ) {
                Map.Entry<String, MapDifference.ValueDifference<Map>> de = di.next();
                MapDifference.ValueDifference<Map> dediff = de.getValue();
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{} type {}: {} has been re-defined between the {} types {} and {}", theConstruct, theFacet, de.getKey(), theConstruct, e.getKey(), theTypeName);
                //for now we just check that the type is consistenly re-declared
                if (!catalog.isDerivedFrom(theFacet.construct(),
                        (String) dediff.leftValue().get("type"),
                        (String) dediff.rightValue().get("type"))) {
                    theContext.addError(
                            theConstruct + TYPE + theFacet + ", redefiniton changed its type: " + de.getKey() + " has been re-defined between the " + theConstruct + " types " + e.getKey() + " and " + theTypeName + " in an incompatible manner", null);
                    res = false;
                }
            }
        }

        return res;
    }

    /* Augmentation occurs in cases such as the declaration of capabilities within a node type.
     * In such cases the construct facets (the capabilitity's properties) can redefine (augment) the
     * specification found in the construct type.
     */
    public boolean checkFacetAugmentation(Construct theConstruct,
                                          Map theSpec,
                                          Facet theFacet,
                                          Checker.CheckContext theContext,
                                          Catalog catalog) {
        return checkFacetAugmentation(theConstruct, theSpec, null, theFacet, theContext, catalog);
    }

    public boolean checkFacetAugmentation(Construct theConstruct,
                                          Map theSpec,
                                          String theSpecType,
                                          Facet theFacet,
                                          Checker.CheckContext theContext,
                                          Catalog catalog) {

        Map<String, Map> augs = (Map<String, Map>) theSpec.get(theFacet.name());
        if (null == augs) {
            return true;
        }

        boolean res = true;
        if (theSpecType == null) {
            theSpecType = (String) theSpec.get("type");
        }
        if (theSpecType == null) {
            theContext.addError("No specification type available", null);
            return false;
        }

        for (Iterator<Map.Entry<String, Map>> ai = augs.entrySet().iterator(); ai.hasNext(); ) {
            Map.Entry<String, Map> ae = ai.next();

            //make sure it was declared by the type
            Map facetDef = catalog.getFacetDefinition(theConstruct, theSpecType, theFacet, ae.getKey());
            if (facetDef == null) {
                theContext.addError(UNKNOWN + theConstruct + " " + theFacet + " (not declared by the type " + theSpecType + ") were used: " + ae.getKey(), null);
                res = false;
                continue;
            }

            //check the compatibility of the augmentation: only the type cannot be changed
            //can the type be changed in a compatible manner ??
            if (!facetDef.get("type").equals(ae.getValue().get("type"))) {
                theContext.addError(theConstruct + " " + theFacet + " " + ae.getKey() + " has a different type than its definition: " + ae.getValue().get("type") + " instead of " + facetDef.get("type"), null);
                res = false;
                continue;
            }
            DataCommon dataCommon = DataCommon.getInstance();
            //check any valuation (here just defaults)
            Object defaultValue = ae.getValue().get(DEFAULT);
            if (defaultValue != null) {
                dataCommon.checkDataValuation(defaultValue, ae.getValue(), theContext);
            }
        }

        return res;
    }

    /*
     * Checks the validity of a certain facet of a construct
     * (properties of a node) across a type hierarchy.
     * For now the check is limited to a verifying that a a facet was declared
     * somewhere in the construct type hierarchy (a node template property has
     * been declared in the node type hierarchy).
     *
     * 2 versions with the more generic allowing the specification of the type
     * to be done explicitly.
     */
    public boolean checkFacet(Construct theConstruct,
                               Map theSpec,
                               Facet theFacet,
                               Checker.CheckContext theContext,
                               Catalog catalog) {
        return checkFacet(theConstruct, theSpec, null, theFacet, theContext, catalog);
    }

    /**
     * We walk the hierarchy and verify the assignment of a property with respect to its definition.
     * We also collect the names of those properties defined as required but for which no assignment was provided.
     */
    public boolean checkFacet(Construct theConstruct,
                               Map theSpec,
                               String theSpecType,
                               Facet theFacet,
                               Checker.CheckContext theContext,
                               Catalog catalog) {

        Map<String, Map> defs = (Map<String, Map>) theSpec.get(theFacet.name());
        if (null == defs) {
            return true;
        }
        defs = Maps.newHashMap(defs); //

        boolean res = true;
        if (theSpecType == null) {
            theSpecType = (String) theSpec.get("type");
        }
        if (theSpecType == null) {
            theContext.addError("No specification type available", null);
            return false;
        }

        Map<String, Byte> missed = new HashMap<>();  //keeps track of the missing required properties, the value is
        //false if a default was found along the hierarchy
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theConstruct, theSpecType);
        while (i.hasNext() && !defs.isEmpty()) {
            Map.Entry<String, Map> type = i.next();

            Map<String, Map> typeDefs = (Map<String, Map>) type.getValue()
                    .get(theFacet.name());
            if (null == typeDefs) {
                continue;
            }

            MapDifference<String, Map> diff = Maps.difference(defs, typeDefs);

            //this are the ones this type and the spec have in common (same key,
            //different values)
            Map<String, MapDifference.ValueDifference<Map>> facetDefs =
                    diff.entriesDiffering();
            //TODO: this assumes the definition of the facet is not cumulative, i.e.
            //subtypes 'add' something to the definition provided by the super-types
            //it considers the most specialized definition stands on its own
            for (MapDifference.ValueDifference<Map> valdef : facetDefs.values()) {
                DataCommon dataCommon = DataCommon.getInstance();
                dataCommon.checkDataValuation(valdef.leftValue(), valdef.rightValue(), theContext);
            }

            //remove from properties all those that appear in this type: unfortunately this returns an unmodifiable map ..
            defs = Maps.newHashMap(diff.entriesOnlyOnLeft());
        }

        if (!defs.isEmpty()) {
            theContext.addError(UNKNOWN + theConstruct + " " + theFacet + " (not declared by the type " + theSpecType + ") were used: " + defs, null);
            res = false;
        }

        if (!missed.isEmpty()) {
            List missedNames =
                    missed.entrySet()
                            .stream()
                            .filter(e -> e.getValue().byteValue() == (byte) 1)
                            .map(e -> e.getKey())
                            .collect(Collectors.toList());
            if (!missedNames.isEmpty()) {
                theContext.addError(theConstruct + " " + theFacet + " missing required values for: " + missedNames, null);
                res = false;
            }
        }

        return res;
    }


}

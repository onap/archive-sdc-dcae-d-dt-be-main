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

import org.onap.sdc.dcae.checker.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;

public class GroupCommon extends BaseCommon{
    private static GroupCommon instance;

    public synchronized static GroupCommon getInstance() {
        if (instance == null)
        {
            instance = new GroupCommon();
        }
        return instance;
    }

    private GroupCommon() {}

    public void checkGroupTypeDefinition(String theName,
                                         Map theDefinition,
                                         Checker.CheckContext theContext,
                                         Catalog catalog) {
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName, Construct.Group);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Group, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }

            if (theDefinition.containsKey(TARGETS_CONSTANT)) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog,
                        ((List<String>) theDefinition.get(TARGETS_CONSTANT)).toArray(EMPTY_STRING_ARRAY));
            }
            InterfaceCommon interfaceCommon = InterfaceCommon.getInstance();
            //interfaces
            Map<String, Map> interfaces =
                    (Map<String, Map>) theDefinition.get(INTERFACES);
            interfaceCommon.checkMapTypeInterfaceDefinition(theContext, interfaces, catalog);

        } finally {
            theContext.exit();
        }
    }

    public void checkGroupDefinition(String theName,
                                     Map theDef,
                                     Checker.CheckContext theContext,
                                     Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!typeCommon.checkType(Construct.Group, theDef, theContext, catalog)) {
                return;
            }

            if (!facetCommon.checkFacet(
                    Construct.Group, theDef, Facet.properties, theContext, catalog)) {
                return;
            }

            if (theDef.containsKey(TARGETS_CONSTANT)) {

                List<String> targetsTypes = (List<String>)
                        catalog.getTypeDefinition(Construct.Group,
                                (String) theDef.get("type"))
                                .get(TARGETS_CONSTANT);

                List<String> targets = (List<String>) theDef.get(TARGETS_CONSTANT);
                for (String targetItr : targets) {
                    if (!catalog.hasTemplate(theContext.target(), Construct.Node, targetItr)) {
                        theContext.addError("The 'targets' entry must contain a reference to a node template, '" + targetItr + "' is not one", null);
                    } else {
                        if (targetsTypes != null) {
                            String targetType = (String)
                                    catalog.getTemplate(theContext.target(), Construct.Node, targetItr).get("type");

                            boolean found = false;
                            for (String type : targetsTypes) {
                                found = catalog
                                        .isDerivedFrom(Construct.Node, targetType, type);
                                if (found) {
                                    break;
                                }
                            }

                            if (!found) {
                                theContext.addError("The 'targets' entry '" + targetItr + "' is not type compatible with any of types specified in policy type targets", null);
                            }
                        }
                    }
                }
            }
        } finally {
            theContext.exit();
        }
    }

}

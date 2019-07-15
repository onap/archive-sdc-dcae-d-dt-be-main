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

import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.IS_NONE_OF_THOSE;
import static org.onap.sdc.dcae.checker.common.ConstCommon.PROPERTIES;
import static org.onap.sdc.dcae.checker.common.ConstCommon.TARGETS_CONSTANT;

public class PolicyCommon extends BaseCommon {

    private static PolicyCommon instance;

    public synchronized static PolicyCommon getInstance() {
        if (instance == null)
        {
            instance = new PolicyCommon();
        }
        return instance;
    }

    private PolicyCommon() {}


    public void checkPolicyTypeDefinition(String theName,
                                          Map theDefinition,
                                          Checker.CheckContext theContext,
                                          Catalog catalog,
                                          Target target) {
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(theName, Construct.Policy);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Policy, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }

            //the targets can be known node types or group types
            List<String> targets = (List<String>) theDefinition.get(TARGETS_CONSTANT);
            if ((targets != null) && (CheckCommon.getInstance().checkDefinition(TARGETS_CONSTANT, targets, theContext))) {
                for (String targetItr : targets) {
                    if (!(catalog.hasType(Construct.Node, targetItr) ||
                            catalog.hasType(Construct.Group, targetItr))) {
                        theContext.addError("The 'targets' entry must contain a reference to a node type or group type, '" + target + IS_NONE_OF_THOSE, null);
                    }
                }
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkPolicyDefinition(String theName,
                                      Map theDef,
                                      Checker.CheckContext theContext,
                                      Catalog catalog,
                                      Target target)
    {
        FacetCommon facetCommon = FacetCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();

        theContext.enter(theName);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!typeCommon.checkType(Construct.Policy, theDef, theContext, catalog)) {
                return;
            }

            if (!facetCommon.checkFacet(Construct.Policy, theDef, Facet.properties, theContext, catalog)) {
                return;
            }

            //targets: must point to node or group templates (that are of a type
            //specified in the policy type definition, if targets were specified
            //there).
            if (theDef.containsKey(TARGETS_CONSTANT)) {
                List<String> targetsTypes = (List<String>)
                        catalog.getTypeDefinition(Construct.Policy,
                                (String) theDef.get("type"))
                                .get(TARGETS_CONSTANT);

                List<String> targets = (List<String>) theDef.get(TARGETS_CONSTANT);
                for (String targetItr : targets) {
                    Construct targetConstruct = null;

                    if (catalog.hasTemplate(theContext.target(), Construct.Group, targetItr)) {
                        targetConstruct = Construct.Group;
                    } else if (catalog.hasTemplate(theContext.target(), Construct.Node, targetItr)) {
                        targetConstruct = Construct.Node;
                    } else {
                        theContext.addError("The 'targets' entry must contain a reference to a node template or group template, '" + target + IS_NONE_OF_THOSE, null);
                    }

                    if (targetConstruct != null &&
                            targetsTypes != null) {
                        //get the target type and make sure is compatible with the types
                        //indicated in the type spec
                        String targetType = (String)
                                catalog.getTemplate(theContext.target(), targetConstruct, targetItr).get("type");

                        boolean found = false;
                        for (String type : targetsTypes) {
                            found = catalog
                                    .isDerivedFrom(targetConstruct, targetType, type);
                            if (found) {
                                break;
                            }
                        }

                        if (!found) {
                            theContext.addError("The 'targets' " + targetConstruct + " entry '" + targetItr + "' is not type compatible with any of types specified in policy type targets", null);
                        }
                    }
                }
            }

        } finally {
            theContext.exit();
        }
    }
}

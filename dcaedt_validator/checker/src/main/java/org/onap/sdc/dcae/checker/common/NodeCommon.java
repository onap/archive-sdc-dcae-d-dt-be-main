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

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;

public class NodeCommon extends BaseCommon {

    private static NodeCommon instance;

    public synchronized static void init(IChecker checker) {
        if (instance == null) {
            instance = new NodeCommon();
            instance.setChecker(checker);
        }
    }

    public synchronized static NodeCommon getInstance() {
        if (instance == null)
        {
            errLogger.log(LogLevel.ERROR, InterfaceCommon.class.getSimpleName(),"Need to call init before");
        }
        return instance;
    }

    private NodeCommon() {}

    private IChecker checker;

    public void checkNodeTypeDefinition(String theName,
                                        Map theDefinition,
                                        Checker.CheckContext theContext,
                                        Catalog catalog) {
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        RequirementCommon requirementCommon = RequirementCommon.getInstance();
        CapabilityCommon capabilityCommon = CapabilityCommon.getInstance();
        InterfaceCommon interfaceCommon = InterfaceCommon.getInstance();
        theContext.enter(theName, Construct.Node);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Node, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Node, theName, theDefinition,
                        Facet.attributes, theContext, catalog);
            }

            //requirements
            if (theDefinition.containsKey(REQUIREMENTS)) {
                requirementCommon.check_requirements(
                        (List<Map>) theDefinition.get(REQUIREMENTS), theContext, catalog);
            }

            //capabilities
            if (theDefinition.containsKey(CAPABILITIES)) {
                capabilityCommon.check_capabilities(
                        (Map<String, Map>) theDefinition.get(CAPABILITIES), theContext, catalog);
            }

            //interfaces:
            Map<String, Map> interfaces =
                    (Map<String, Map>) theDefinition.get(INTERFACES);
            interfaceCommon.checkMapTypeInterfaceDefinition(theContext, interfaces, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    public void checkNodeTemplateDefinition(String theName,
                                             Map theNode,
                                             Checker.CheckContext theContext,
                                             Catalog catalog) {
        TypeCommon typeCommon = TypeCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        RequirementCommon requirementCommon = RequirementCommon.getInstance();
        CapabilityCommon capabilityCommon = CapabilityCommon.getInstance();
        InterfaceCommon interfaceCommon = InterfaceCommon.getInstance();
        theContext.enter(theName, Construct.Node);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theNode, theContext)) {
                return;
            }

            if (!typeCommon.checkType(Construct.Node, theNode, theContext, catalog)) {
                return;
            }

            //copy
            String copy = (String) theNode.get("copy");
            if (copy != null) {
                if (!typeCommon.checkTemplateReference(Construct.Node, theContext, catalog, copy)) {
                    theContext.addError("The 'copy' reference " + copy + " does not point to a known node template", null);
                } else {
                    //the 'copy' node specification should be used to provide 'defaults'
                    //for this specification
                }
            }

      /* check that we operate on properties and attributes within the scope of
        the specified node type */
            if (!facetCommon.checkFacet(
                    Construct.Node, /*theName,*/theNode, Facet.properties, theContext, catalog)) {
                return;
            }

            if (!facetCommon.checkFacet(
                    Construct.Node, /*theName,*/theNode, Facet.attributes, theContext, catalog)) {
                return;
            }

            //requirement assignment seq
            if (theNode.containsKey(REQUIREMENTS)) {
                requirementCommon.checkRequirementsAssignmentDefinition(
                        (List<Map>) theNode.get(REQUIREMENTS), theContext, catalog);
            }

            //capability assignment map: subject to augmentation
            if (theNode.containsKey(CAPABILITIES)) {
                capabilityCommon.checkCapabilitiesAssignmentDefinition(
                        (Map<String, Map>) theNode.get(CAPABILITIES), theContext, catalog);
            }

            //interfaces
            if (theNode.containsKey(INTERFACES)) {
                interfaceCommon.checkTemplateInterfacesDefinition(
                        (Map<String, Map>) theNode.get(INTERFACES), theContext, catalog);
            }

            //artifacts: artifacts do not have different definition forms/syntax
            //depending on the context (type or template) but they are still subject
            //to 'augmentation'
            if (theNode.containsKey(ARTIFACTS)) {
                checker.check_template_artifacts_definition(
                        (Map<String, Object>) theNode.get(ARTIFACTS), theContext);
            }

            /* node_filter: the context to which the node filter is applied is very
             * wide here as opposed to the node filter specification in a requirement
             * assignment which has a more strict context (target node/capability are
             * specified).
             * We could check that there are nodes in this template having the
             * properties/capabilities specified in this filter, i.e. the filter has
             * a chance to succeed.
             */
        } finally {
            theContext.exit();
        }
    }

    /* Requirements are the odd ball as they are structured as a sequence .. */
    public Map<String, Map> findNodeTypeRequirementByName(
            String theNodeType, String theRequirementName, Catalog catalog) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findNodeTypeRequirementByName {}/{}", theNodeType, theRequirementName);
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(Construct.Node, theNodeType);
        while (i.hasNext()) {
            Map.Entry<String, Map> nodeType = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findNodeTypeRequirementByName, Checking node type {}", nodeType.getKey());
            List<Map<String, Map>> nodeTypeRequirements =
                    (List<Map<String, Map>>) nodeType.getValue().get(REQUIREMENTS);
            if (nodeTypeRequirements == null) {
                continue;
            }

            for (Map<String, Map> requirement : nodeTypeRequirements) {
                Map requirementDef = requirement.get(theRequirementName);
                if (requirementDef != null) {
                    return requirementDef;
                }
            }
        }
        return null;
    }

    public void setChecker(IChecker checker) {
        this.checker = checker;
    }
}

package org.onap.sdc.dcae.checker.common;

import org.apache.commons.jxpath.JXPathContext;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.*;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;

public class RequirementCommon extends BaseCommon {

    private static RequirementCommon instance;

    public synchronized static RequirementCommon getInstance() {
        if (instance == null)
        {
            instance = new RequirementCommon();
        }
        return instance;
    }

    private RequirementCommon() {}

    public void check_requirements(List<Map> theDefinition,
                                   Checker.CheckContext theContext,
                                   Catalog catalog) {
        theContext.enter(REQUIREMENTS);
        try {
            if (!CheckCommon.getInstance().checkDefinition(REQUIREMENTS, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map> i = theDefinition.iterator(); i.hasNext(); ) {
                Map e = i.next();
                Iterator<Map.Entry<String, Map>> ei =
                        (Iterator<Map.Entry<String, Map>>) e.entrySet().iterator();
                Map.Entry<String, Map> eie = ei.next();
                checkRequirementDefinition(eie.getKey(), eie.getValue(), theContext, catalog);
                assert !ei.hasNext();
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkRequirementDefinition(String theName,
                                            Map theDef,
                                            Checker.CheckContext theContext,
                                            Catalog catalog) {
        TypeCommon typeCommon = TypeCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(theName, Construct.Requirement);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }
            //check capability type
            String capabilityType = (String) theDef.get(CAPABILITY);
            if (null != capabilityType) {
                typeCommon.checkTypeReference(Construct.Capability, theContext, catalog, capabilityType);
            }

            //check node type
            String nodeType = (String) theDef.get("node");
            if (null != nodeType) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog, nodeType);
            }

            //check relationship type
            Map relationshipSpec = (Map) theDef.get("relationship");
            String relationshipType = null;
            if (null != relationshipSpec) {
                relationshipType = (String) relationshipSpec.get("type");
                if (relationshipType != null) { //should always be the case
                    typeCommon.checkTypeReference(Construct.Relationship, theContext, catalog, relationshipType);
                }

                Map<String, Map> interfaces = (Map<String, Map>)
                        relationshipSpec.get(INTERFACES);
                if (interfaces != null) {
                    //augmentation (additional properties or operations) of the interfaces
                    //defined by the above relationship types

                    //check that the interface types are known
                    for (Map interfaceDef : interfaces.values()) {
                        typeCommon.checkType(Construct.Interface, interfaceDef, theContext, catalog);
                    }
                }
            }

            //cross checks

            //the capability definition might come from the capability type or from the capability definition
            //within the node type. We might have more than one as a node might specify multiple capabilities of the
            //same type.
            //the goal here is to cross check the compatibility of the valid_source_types specification in the
            //target capability definition (if that definition contains a valid_source_types entry).
            List<Map> capabilityDefs = new LinkedList<>();
            //nodeType exposes capabilityType
            if (nodeType != null) {
                Map<String, Map> capabilities =
                        facetCommon.findTypeFacetByType(Construct.Node, nodeType,
                                Facet.capabilities, capabilityType, catalog);
                if (capabilities.isEmpty()) {
                    theContext.addError("The node type " + nodeType + " does not appear to expose a capability of a type compatible with " + capabilityType, null);
                } else {
                    for (Map.Entry<String, Map> capability : capabilities.entrySet()) {
                        //this is the capability as it was defined in the node type
                        Map capabilityDef = capability.getValue();
                        //if it defines a valid_source_types then we're working with it,
                        //otherwise we're working with the capability type it points to.
                        //The spec does not make it clear if the valid_source_types in a capability definition augments or
                        //overwrites the one from the capabilityType (it just says they must be compatible).
                        if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                            capabilityDefs.add(capabilityDef);
                        } else {
                            capabilityDef =
                                    catalog.getTypeDefinition(Construct.Capability, (String) capabilityDef.get("type"));
                            if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                                capabilityDefs.add(capabilityDef);
                            } else {
                                //!!if there is a capability that does not have a valid_source_type than there is no reason to
                                //make any further verification (as there is a valid node_type/capability target for this requirement)
                                capabilityDefs.clear();
                                break;
                            }
                        }
                    }
                }
            } else {
                Map capabilityDef = catalog.getTypeDefinition(Construct.Capability, capabilityType);
                if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                    capabilityDefs.add(capabilityDef);
                }
            }

            //check that the node type enclosing this requirement definition
            //is in the list of valid_source_types
            if (!capabilityDefs.isEmpty()) {
                String enclosingNodeType =
                        theContext.enclosingConstruct(Construct.Node);
                assert enclosingNodeType != null;

                if (!capabilityDefs.stream().anyMatch(
                        (Map capabilityDef) -> {
                            List<String> valid_source_types =
                                    (List<String>) capabilityDef.get(VALID_SOURCE_TYPES);
                            return valid_source_types.stream().anyMatch(
                                    (String source_type) -> catalog.isDerivedFrom(
                                            Construct.Node, enclosingNodeType, source_type));
                        })) {
                    theContext.addError("Node type: " + enclosingNodeType + " not compatible with any of the valid_source_types provided in the definition of compatible capabilities", null);
                }
            }

            //if we have a relationship type, check if it has a valid_target_types
            //if it does, make sure that the capability type is compatible with one
            //of them
            if (relationshipType != null) { //should always be the case
                Map relationshipTypeDef = catalog.getTypeDefinition(
                        Construct.Relationship, relationshipType);
                if (relationshipTypeDef != null) {
                    List<String> valid_target_types =
                            (List<String>) relationshipTypeDef.get(VALID_TARGET_TYPES);
                    if (valid_target_types != null) {
                        boolean found = false;
                        for (String target_type : valid_target_types) {
                            if (catalog.isDerivedFrom(
                                    Construct.Capability, capabilityType, target_type)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            theContext.addError("Capability type: " + capabilityType + " not compatible with any of the valid_target_types " + valid_target_types + " provided in the definition of relationship type " + relationshipType, null);
                        }
                    }
                }
            }

            //relationship declares the capabilityType in its valid_target_type set
            //in A.6.9 'Relationship Type' the spec does not indicate how	inheritance
            //is to be applied to the valid_target_type spec: cumulative, overwrites,
            //so we treat it as an overwrite.
        } finally {
            theContext.exit();
        }
    }

    //requirements and capabilities assignment appear in a node templates
    public void checkRequirementsAssignmentDefinition(
            List<Map> theRequirements, Checker.CheckContext theContext, Catalog catalog) {
        NodeCommon nodeCommon = NodeCommon.getInstance();
        theContext.enter(REQUIREMENTS);
        try {
            if (!CheckCommon.getInstance().checkDefinition(REQUIREMENTS, theRequirements, theContext)) {
                return;
            }

            //the node type for the node template enclosing these requirements
            String nodeType = (String) catalog.getTemplate(
                    theContext.target(),
                    Construct.Node,
                    theContext.enclosingConstruct(Construct.Node))
                    .get("type");

            for (Iterator<Map> ri = theRequirements.iterator(); ri.hasNext(); ) {
                Map<String, Map> requirement = (Map<String, Map>) ri.next();

                Iterator<Map.Entry<String, Map>> rai = requirement.entrySet().iterator();

                Map.Entry<String, Map> requirementEntry = rai.next();
                assert !rai.hasNext();

                String requirementName = requirementEntry.getKey();
                Map requirementDef = nodeCommon.findNodeTypeRequirementByName(
                        nodeType, requirementName, catalog);

                if (requirementDef == null) {
                    theContext.addError("No requirement " + requirementName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkRequirementAssignmentDefinition(
                        requirementName, requirementEntry.getValue(), requirementDef, theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkRequirementAssignmentDefinition(
            String theRequirementName,
            Map theAssignment,
            Map theDefinition,
            Checker.CheckContext theContext,
            Catalog catalog) {
        TypeCommon typeCommon = TypeCommon.getInstance();
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext//.enter("requirement_assignment")
                .enter(theRequirementName, Construct.Requirement);

        //grab the node type definition to verify compatibility

        try {
            //node assignment
            boolean targetNodeIsTemplate = false;
            String targetNode = (String) theAssignment.get("node");
            if (targetNode == null) {
                targetNode = (String) theDefinition.get("node");
                //targetNodeIsTemplate stays false, targetNode must be a type
            } else {
                //the value must be a node template or a node type
                targetNodeIsTemplate = typeCommon.isTemplateReference(
                        Construct.Node, theContext, targetNode, catalog);
                if ((!targetNodeIsTemplate) && (!typeCommon.isTypeReference(Construct.Node, targetNode, catalog))){
                    theContext.addError("The 'node' entry must contain a reference to a node template or node type, '" + targetNode + IS_NONE_OF_THOSE, null);
                    return;
                }

                //additional checks
                String targetNodeDef = (String) theDefinition.get("node");
                if (targetNodeDef != null && targetNode != null) {
                    if (targetNodeIsTemplate) {
                        //if the target is node template, it must be compatible with the
                        //node type specification in the requirement defintion
                        String targetNodeType = (String)
                                catalog.getTemplate(theContext.target(), Construct.Node, targetNode).get("type");
                        if (!catalog.isDerivedFrom(
                                Construct.Node, targetNodeType, targetNodeDef)) {
                            theContext.addError("The required target node type '" + targetNodeType + "' of target node " + targetNode + " is not compatible with the target node type found in the requirement definition: " + targetNodeDef, null);
                            return;
                        }
                    } else {
                        //if the target is a node type it must be compatible (= or derived
                        //from) with the node type specification in the requirement definition
                        if (!catalog.isDerivedFrom(
                                Construct.Node, targetNode, targetNodeDef)) {
                            theContext.addError("The required target node type '" + targetNode + "' is not compatible with the target node type found in the requirement definition: " + targetNodeDef, null);
                            return;
                        }
                    }
                }
            }

            String targetNodeType = targetNodeIsTemplate ?
                    (String) catalog.getTemplate(theContext.target(), Construct.Node, targetNode).get("type") :
                    targetNode;

            //capability assignment
            boolean targetCapabilityIsType = false;
            String targetCapability = (String) theAssignment.get(CAPABILITY);
            if (targetCapability == null) {
                targetCapability = (String) theDefinition.get(CAPABILITY);
                //in a requirement definition the target capability can only be a
                //capability type (and not a capability name within some target node
                //type)
                targetCapabilityIsType = true;
            } else {
                targetCapabilityIsType = typeCommon.isTypeReference(Construct.Capability, targetCapability, catalog);

                //check compatibility with the target compatibility type specified
                //in the requirement definition, if any
                String targetCapabilityDef = (String) theDefinition.get(CAPABILITY);
                if (targetCapabilityDef != null && targetCapability != null) {
                    if (targetCapabilityIsType) {
                        if (!catalog.isDerivedFrom(
                                Construct.Capability, targetCapability, targetCapabilityDef)) {
                            theContext.addError("The required target capability type '" + targetCapability + "' is not compatible with the target capability type found in the requirement definition: " + targetCapabilityDef, null);
                            return;
                        }
                    } else {
                        //the capability is from a target node. Find its definition and
                        //check that its type is compatible with the capability type
                        //from the requirement definition

                        //check target capability compatibility with target node
                        if (targetNode == null) {
                            theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', which was not specified", null);
                            return;
                        }
                        if (!targetNodeIsTemplate) {
                            theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', but there you specified a node type", null);
                            return;
                        }
                        //check that the targetNode (its type) indeed has the
                        //targetCapability

                        Map<String, Object> targetNodeCapabilityDef =
                                facetCommon.findTypeFacetByName(
                                        Construct.Node, targetNodeType,
                                        Facet.capabilities, targetCapability, catalog);
                        if (targetNodeCapabilityDef == null) {
                            theContext.addError("No capability '" + targetCapability + "' was specified in the node " + targetNode + " of type " + targetNodeType, null);
                            return;
                        }

                        String targetNodeCapabilityType = (String) targetNodeCapabilityDef.get("type");

                        if (!catalog.isDerivedFrom(Construct.Capability,
                                targetNodeCapabilityType,
                                targetCapabilityDef)) {
                            theContext.addError("The required target capability type '" + targetCapabilityDef + "' is not compatible with the target capability type found in the target node type capability definition : " + targetNodeCapabilityType + ", targetNode " + targetNode + ", capability name " + targetCapability, null);
                            return;
                        }
                    }
                }
            }

            //relationship assignment
            Map targetRelationship = (Map) theAssignment.get("relationship");
            if (targetRelationship != null) {
                //this has to be compatible with the relationship with the same name
                //from the node type
                //check the type
            }

            //node_filter; used jxpath to simplify the navigation somewhat
            //this is too cryptic
            JXPathContext jxPath = JXPathContext.newContext(theAssignment);
            jxPath.setLenient(true);

            List<Map> propertiesFilter =
                    (List<Map>) jxPath.getValue("/node_filter/properties");
            if (propertiesFilter != null) {
                for (Map propertyFilter : propertiesFilter) {
                    if (targetNode != null) {
                        //if we have a target node or node template then it must have
                        //have these properties
                        for (Object propertyName : propertyFilter.keySet()) {
                            if (null == facetCommon.findTypeFacetByName(Construct.Node,
                                    targetNodeType,
                                    Facet.properties,
                                    propertyName.toString(),
                                    catalog)) {
                                theContext.addError("The node_filter property " + propertyName + " is invalid: requirement target node " + targetNode + " does not have such a property", null);
                            }
                        }
                    }
                }
            }

            List<Map> capabilitiesFilter =
                    (List<Map>) jxPath.getValue("node_filter/capabilities");
            if (capabilitiesFilter != null) {
                for (Map capabilityFilterDef : capabilitiesFilter) {
                    assert capabilityFilterDef.size() == 1;
                    Map.Entry<String, Map> capabilityFilterEntry =
                            (Map.Entry<String, Map>) capabilityFilterDef.entrySet().iterator().next();
                    String targetFilterCapability = capabilityFilterEntry.getKey();
                    Map<String, Object> targetFilterCapabilityDef = null;

                    //if we have a targetNode capabilityName must be a capability of
                    //that node (type); or it can be simply capability type (but the node
                    //must have a capability of that type)

                    String targetFilterCapabilityType = null;
                    if (targetNode != null) {
                        targetFilterCapabilityDef =
                                facetCommon.findTypeFacetByName(Construct.Node, targetNodeType,
                                        Facet.capabilities, targetFilterCapability, catalog);
                        if (targetFilterCapabilityDef != null) {
                            targetFilterCapabilityType =
                                    (String) targetFilterCapabilityDef/*.values().iterator().next()*/.get("type");
                        } else {
                            Map<String, Map> targetFilterCapabilities =
                                    facetCommon.findTypeFacetByType(Construct.Node, targetNodeType,
                                            Facet.capabilities, targetFilterCapability, catalog);

                            if (!targetFilterCapabilities.isEmpty()) {
                                if (targetFilterCapabilities.size() > 1) {
                                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "checkRequirementAssignmentDefinition: filter check, target node type '{}' has more than one capability of type '{}', not supported", targetNodeType, targetFilterCapability);
                                }
                                //pick the first entry, it represents a capability of the required type
                                Map.Entry<String, Map> capabilityEntry = targetFilterCapabilities.entrySet().iterator().next();
                                targetFilterCapabilityDef = Collections.singletonMap(capabilityEntry.getKey(),
                                        capabilityEntry.getValue());
                                targetFilterCapabilityType = targetFilterCapability;
                            }
                        }
                    } else {
                        //no node (type) specified, it can be a straight capability type
                        targetFilterCapabilityDef = catalog.getTypeDefinition(
                                Construct.Capability, targetFilterCapability);
                        //here comes the odd part: it can still be a just a name in which
                        //case we should look at the requirement definition, see which
                        //capability (type) it indicates
                        assert targetCapabilityIsType; //cannot be otherwise, we'd need a node
                        targetFilterCapabilityDef = catalog.getTypeDefinition(
                                Construct.Capability, targetCapability);
                        targetFilterCapabilityType = targetCapability;
                    }

                    if (targetFilterCapabilityDef == null) {
                        theContext.addError("Capability (name or type) " + targetFilterCapability + " is invalid: not a known capability (type) " +
                                ((targetNodeType != null) ? (" of node type" + targetNodeType) : ""), null);
                        continue;
                    }

                    for (Map propertyFilter :
                            (List<Map>) jxPath.getValue("/node_filter/capabilities/" + targetFilterCapability + "/properties")) {
                        //check that the properties are in the scope of the
                        //capability definition
                        for (Object propertyName : propertyFilter.keySet()) {
                            if (null == facetCommon.findTypeFacetByName(Construct.Capability,
                                    targetCapability,
                                    Facet.properties,
                                    propertyName.toString(),
                                    catalog)) {
                                theContext.addError("The capability filter " + targetFilterCapability + " property " + propertyName + " is invalid: target capability " + targetFilterCapabilityType + " does not have such a property", null);
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

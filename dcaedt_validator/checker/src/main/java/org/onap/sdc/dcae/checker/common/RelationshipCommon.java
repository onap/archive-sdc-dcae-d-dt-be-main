package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.dcae.checker.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;
import static org.onap.sdc.dcae.checker.common.ConstCommon.EMPTY_STRING_ARRAY;
import static org.onap.sdc.dcae.checker.common.ConstCommon.VALID_TARGET_TYPES;

public class RelationshipCommon extends BaseCommon {
    private static RelationshipCommon instance;

    public synchronized static RelationshipCommon getInstance() {
        if (instance == null)
        {
            instance = new RelationshipCommon();
        }
        return instance;
    }

    private RelationshipCommon() {}

    public void checkRelationshipTypeDefinition(String theName,
                                                Map theDefinition,
                                                Checker.CheckContext theContext,
                                                Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        InterfaceCommon interfaceCommon = InterfaceCommon.getInstance();
        theContext.enter(theName, Construct.Relationship);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
                        Facet.attributes, theContext, catalog);
            }

            Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get(INTERFACES);
            if (interfaces != null) {
                theContext.enter(INTERFACES);
                for (Iterator<Map.Entry<String, Map>> i =
                     interfaces.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, Map> e = i.next();
                    interfaceCommon.check_type_interface_definition(
                            e.getKey(), e.getValue(), theContext, catalog);
                }
                theContext.exit();
            }

            if (theDefinition.containsKey(VALID_TARGET_TYPES)) {
                typeCommon.checkTypeReference(Construct.Capability, theContext , catalog,
                        ((List<String>) theDefinition.get(VALID_TARGET_TYPES)).toArray(EMPTY_STRING_ARRAY));
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkRelationshipTemplateDefinition(
            String theName,
            Map theRelationship,
            Checker.CheckContext theContext,
            Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName, Construct.Relationship);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theRelationship, theContext)) {
                return;
            }

            if (!typeCommon.checkType(Construct.Relationship, theRelationship, theContext, catalog)) {
                return;
            }

      /* check that we operate on properties and attributes within the scope of
        the specified relationship type */
            if (!facetCommon.checkFacet(Construct.Relationship, theRelationship,
                    Facet.properties, theContext, catalog)) {
                return;
            }

            if (!facetCommon.checkFacet(Construct.Relationship, theRelationship,
                    Facet.attributes, theContext, catalog)) {
                return;
            }

    /* interface definitions
           note: augmentation is allowed here so not clear what to check ..
             maybe report augmentations if so configured .. */

        } finally {
            theContext.exit();
        }
    }
}

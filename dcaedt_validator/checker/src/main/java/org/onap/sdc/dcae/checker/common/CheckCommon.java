package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.dcae.checker.*;

import java.util.List;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.PROPERTIES;

public class CheckCommon extends BaseCommon {
    private static CheckCommon instance = null;

    public synchronized static CheckCommon getInstance() {
        if (instance == null)
        {
            instance = new CheckCommon();
        }
        return instance;
    }

    private CheckCommon() {}

    public boolean checkDefinition(String theName,
                               Map theDefinition,
                               Checker.CheckContext theContext) {
    if (theDefinition == null) {
        theContext.addError("Missing definition for " + theName, null);
        return false;
    }

    if (theDefinition.isEmpty()) {
        theContext.addError("Empty definition for " + theName, null);
        return false;
    }

    return true;
    }

    public boolean checkDefinition(String theName,
                                    List theDefinition,
                                    Checker.CheckContext theContext) {
        if (theDefinition == null) {
            theContext.addError("Missing definition for " + theName, null);
            return false;
        }

        if (theDefinition.isEmpty()) {
            theContext.addError("Empty definition for " + theName, null);
            return false;
        }

        return true;
    }

    public void checkDataTypeDefinition(String theName,
                                        Map theDefinition,
                                        Checker.CheckContext theContext,
                                        Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        PropertiesCommon propertiesCommon = PropertiesCommon.getInstance();
        theContext.enter(theName, Construct.Data);
        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                propertiesCommon.checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Data, theName, theDefinition,
                        Facet.properties, theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }


}

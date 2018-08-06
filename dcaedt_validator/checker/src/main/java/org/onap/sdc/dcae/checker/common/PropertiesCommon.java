package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.dcae.checker.Catalog;
import org.onap.sdc.dcae.checker.Checker;
import org.onap.sdc.dcae.checker.IChecker;

import java.util.Iterator;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.DEFAULT;
import static org.onap.sdc.dcae.checker.common.ConstCommon.PROPERTIES;

public class PropertiesCommon extends BaseCommon {

    private static PropertiesCommon instance;

    public synchronized static PropertiesCommon getInstance() {
        if (instance == null)
        {
            instance = new PropertiesCommon();
        }
        return instance;
    }

    private PropertiesCommon() {}

    public void checkProperties(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext, Catalog catalog) {
        theContext.enter(PROPERTIES);
        try {
            if (!CheckCommon.getInstance().checkDefinition(PROPERTIES, theDefinitions, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkPropertyDefinition(e.getKey(), e.getValue(), theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkPropertyDefinition(
            String theName, Map theDefinition, Checker.CheckContext theContext, Catalog catalog) {
        DataCommon dataCommon = DataCommon.getInstance();
        theContext.enter(theName);
        if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
            return;
        }
        //check the type
        if (!dataCommon.checkDataType(theDefinition, theContext, catalog)) {
            return;
        }
        //check default value is compatible with type
        Object defaultValue = theDefinition.get(DEFAULT);
        if (defaultValue != null) {
            dataCommon.checkDataValuation(defaultValue, theDefinition, theContext);
        }

        theContext.exit();
    }

    public void check_property_definition(String theName, Map theDefinition, Checker.CheckContext theContext, Catalog catalog) {
        DataCommon dataCommon = DataCommon.getInstance();
        theContext.enter(theName);
        if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
            return;
        }
        // check the type
        if (!dataCommon.checkDataType(theDefinition, theContext, catalog)) {
            return;
        }
        // check default value is compatible with type
        Object defaultValue = theDefinition.get("default");
        if (defaultValue != null) {
            dataCommon.checkDataValuation(defaultValue, theDefinition, theContext);
        }

        theContext.exit();
    }

    public void check_properties(Map<String, Map> theDefinitions, Checker.CheckContext theContext, Catalog catalog) {
        theContext.enter("properties");
        try {
            if (!CheckCommon.getInstance().checkDefinition("properties", theDefinitions, theContext))
                return;

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Map> e = i.next();
                check_property_definition(e.getKey(), e.getValue(), theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }
}

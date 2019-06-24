package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;

public class TypeCommon extends BaseCommon {

    private static TypeCommon instance;

    public synchronized static TypeCommon getInstance() {
        if (instance == null)
        {
            instance = new TypeCommon();
        }
        return instance;
    }

    private TypeCommon() {}

    public boolean catalogTypes(Construct theConstruct, Map<String, Map> theTypes, Checker.CheckContext theContext, Catalog catalog) {

        boolean res = true;
        for (Map.Entry<String, Map> typeEntry : theTypes.entrySet()) {
            res &= catalogType(theConstruct, typeEntry.getKey(), typeEntry.getValue(), theContext, catalog);
        }

        return res;
    }

    public boolean catalogType(Construct theConstruct,
                                String theName,
                                Map theDef,
                                Checker.CheckContext theContext,
                                Catalog catalog) {

        if (!catalog.addType(theConstruct, theName, theDef)) {
            theContext.addError(theConstruct + TYPE + theName + " re-declaration", null);
            return false;
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{} type {} has been cataloged", theConstruct, theName);

        String parentType = (String) theDef.get("derived_from");
        if (parentType != null && !catalog.hasType(theConstruct, parentType)) {
            theContext.addError(
                    theConstruct + TYPE + theName + " indicates a supertype that has not (yet) been declared: " + parentType, null);
            return false;
        }
        return true;
    }

    public boolean checkTypeReference(Construct theConstruct,
                                       Checker.CheckContext theContext,
                                       Catalog catalog,
                                       String... theTypeNames) {
        boolean res = true;
        for (String typeName : theTypeNames) {
            if (!isTypeReference(theConstruct, typeName, catalog)) {
                theContext.addError("Reference to " + theConstruct + " type '" + typeName + "' points to unknown type", null);
                res = false;
            }
        }
        return res;
    }

    public boolean isTypeReference(Construct theConstruct,
                                   String theTypeName, Catalog catalog) {
        return catalog.hasType(theConstruct, theTypeName);
    }

    //generic checking actions, not related to validation rules

    /* will check the validity of the type specification for any construct containing a 'type' entry */
    public boolean checkType(Construct theCategory, Map theSpec, Checker.CheckContext theContext, Catalog catalog) {
        String type = (String) theSpec.get("type");
        if (type == null) {
            theContext.addError("Missing type specification", null);
            return false;
        }

        if (!catalog.hasType(theCategory, type)) {
            theContext.addError(UNKNOWN + theCategory + " type: " + type, null);
            return false;
        }

        return true;
    }

    /* node or relationship templates */
    public boolean checkTemplateReference(Construct theConstruct,
                                           Checker.CheckContext theContext,
                                           Catalog catalog,
                                           String... theTemplateNames) {
        boolean res = true;
        for (String templateName : theTemplateNames) {
            if (!isTemplateReference(theConstruct, theContext, templateName, catalog)) {
                theContext.addError("Reference to " + theConstruct + " template '" + templateName + "' points to unknown template", null);
                res = false;
            }
        }
        return res;
    }

    public boolean isTemplateReference(Construct theConstruct,
                                        Checker.CheckContext theContext,
                                        String theTemplateName,
                                        Catalog catalog) {
        return catalog.hasTemplate(theContext.target(), theConstruct, theTemplateName);
    }
}

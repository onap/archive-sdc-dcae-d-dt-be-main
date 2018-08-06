package org.onap.sdc.dcae.checker.common;

import kwalify.Rule;
import kwalify.Validator;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.checker.*;
import org.onap.sdc.dcae.checker.validation.TOSCAValidator;

import java.util.Iterator;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.DEFAULT;

public class InputsOutputsCommon extends BaseCommon {

    private static InputsOutputsCommon instance;

    public synchronized static InputsOutputsCommon getInstance() {
        if (instance == null)
        {
            instance = new InputsOutputsCommon();
        }
        return instance;
    }

    private InputsOutputsCommon() {}

    public void checkInputDefinition(String theName,
                                     Map theDef,
                                     Checker.CheckContext theContext,
                                     Catalog catalog) {
        DataCommon dataCommon = DataCommon.getInstance();
        theContext.enter(theName);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }
            //
            if (!dataCommon.checkDataType(theDef, theContext, catalog)) {
                return;
            }
            //check default value
            Object defaultValue = theDef.get(DEFAULT);
            if (defaultValue != null) {
                dataCommon.checkDataValuation(defaultValue, theDef, theContext);
            }
        } finally {
            theContext.exit();
        }
    }



    public void checkOutputDefinition(String theName,
                                       Map theDef,
                                       Checker.CheckContext theContext) {
        theContext.enter(theName);
        try {
            CheckCommon.getInstance().checkDefinition(theName, theDef, theContext);
            //check the expression
        } finally {
            theContext.exit();
        }
    }

}

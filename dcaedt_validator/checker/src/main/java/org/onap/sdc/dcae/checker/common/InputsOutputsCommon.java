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

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

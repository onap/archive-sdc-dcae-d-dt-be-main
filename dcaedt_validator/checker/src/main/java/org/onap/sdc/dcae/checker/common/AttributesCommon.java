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

import org.onap.sdc.dcae.checker.Catalog;
import org.onap.sdc.dcae.checker.Checker;

import java.util.Iterator;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.ATTRIBUTES;

public class AttributesCommon extends BaseCommon {
    private static AttributesCommon instance;

    public synchronized static AttributesCommon getInstance() {
        if (instance == null)
        {
            instance = new AttributesCommon();
        }
        return instance;
    }

    private AttributesCommon() {}

    public void checkAttributes(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext, Catalog catalog) {
        theContext.enter(ATTRIBUTES);
        try {
            if (!CheckCommon.getInstance().checkDefinition(ATTRIBUTES, theDefinitions, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkAttributeDefinition(e.getKey(), e.getValue(), theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkAttributeDefinition(
            String theName, Map theDefinition, Checker.CheckContext theContext, Catalog catalog) {
        DataCommon dataCommon = DataCommon.getInstance();
        theContext.enter(theName);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }
            if (!dataCommon.checkDataType(theDefinition, theContext, catalog)) {
                return;
            }
        } finally {
            theContext.exit();
        }
    }

    public void check_attributes(Map<String, Map> theDefinitions, Checker.CheckContext theContext, Catalog catalog) {
        theContext.enter("attributes");
        try {
            if (!CheckCommon.getInstance().checkDefinition("attributes", theDefinitions, theContext))
                return;

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Map> e = i.next();
                checkAttributeDefinition(e.getKey(), e.getValue(), theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

}

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

import org.onap.sdc.dcae.checker.Checker;
import org.onap.sdc.dcae.checker.Construct;

import java.util.Map;

public class ArtifactCommon extends BaseCommon {

    private static ArtifactCommon instance;

    public synchronized static ArtifactCommon getInstance() {
        if (instance == null)
        {
            instance = new ArtifactCommon();
        }
        return instance;
    }

    private ArtifactCommon() {}
    public void checkArtifactTypeDefinition(String theName,
                                             Map theDefinition,
                                             Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Artifact);
        try {
            CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext);
        } finally {
            theContext.exit();
        }
    }
}

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

package utilities;

import json.Environment;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;

import java.util.List;
import java.util.Map;

public interface IDcaeRestClient {
    void init(Environment environment);

    String getUserId();

    List<ResourceDetailed> getAllVfcmts();

    List<ResourceDetailed> getAllBaseVfcmts();

    ResourceDetailed createResource(CreateVFCMTRequest resource);

    ResourceDetailed checkoutVfcmt(String vfcmtUuid);

    ResourceDetailed checkinVfcmt(String vfcmtUuid);

	Map<String, List<Resource>> getDcaeCatalog();

    String getItemModel(String elementId);

    String getItemType(String elementId, String type);

    String saveComposition(String componentId, String body);

    String certifyVfcmt(String vfcmtUuid);

    void updateResource(ResourceDetailed vfcmt);
}

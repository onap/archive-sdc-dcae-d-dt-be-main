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

package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class NodeData {
    private final JsonArray capabilities;
    private final JsonArray requirements;
    private final JsonArray properties;
    private final JsonObject typeInfo;
    private final String nodeName;
    private final String aliasBelong;

    NodeData(JsonArray capabilities, JsonArray requirements, JsonArray properties, JsonObject typeInfo, String nodeName, String aliasBelong) {
        this.capabilities = capabilities;
        this.requirements = requirements;
        this.properties = properties;
        this.typeInfo = typeInfo;
        this.nodeName = nodeName;
        this.aliasBelong = aliasBelong;
    }

    public JsonArray getCapabilities() {
        return capabilities;
    }

    public JsonArray getRequirements() {
        return requirements;
    }

    public JsonArray getProperties() {
        return properties;
    }

    public JsonObject getTypeInfo() {
        return typeInfo;
    }

    public String getName() {
        return nodeName;
    }

    public String getNameWithAlias() {
        if (StringUtils.isBlank(aliasBelong)) {
            return nodeName;
        } else {
            return aliasBelong + "." + nodeName;
        }
    }

    public String getAliasBelong() {
        return aliasBelong;
    }
}

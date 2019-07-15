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

package org.onap.sdc.dcae.rule.editor.utils;

import java.util.List;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class RulesPayloadUtils {

    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();

    private static Gson gson = new GsonBuilder().serializeNulls()
            .registerTypeAdapter(BaseAction.class, new ActionDeserializer())
            .registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

    private RulesPayloadUtils(){}

    public static Rule parsePayloadToRule(String payload) {
        return convertFromPayload(payload, Rule.class);
    }

    public static MappingRules parseMappingRulesArtifactPayload(String payload) {
        return convertFromPayload(payload, MappingRules.class);
    }

	public static MappingRulesResponse parsePayloadToMappingRules(String payload) {
		return convertFromPayload(payload, MappingRulesResponse.class);
	}

	public static <T> T convertFromPayload(String payload, Class<T> type) {
		return gson.fromJson(payload, type);
	}

    public static SchemaInfo extractInfoFromDescription(Artifact rulesArtifact) {
        try {
            return gson.fromJson(rulesArtifact.getArtifactDescription(), SchemaInfo.class);
        }catch (JsonParseException e) {
            errLogger.log(LogLevel.ERROR, RulesPayloadUtils.class.getName(), "Exception thrown while parsing rule artifact description: {}", e);
            return null;
        }
    }

    public static String buildSchemaAndRulesResponse(String payload, List<EventTypeDefinitionUI> schema) {
        return  "{\"schema\":"+gson.toJson(schema)+","+payload.replaceFirst("\\{", "");
    }


}

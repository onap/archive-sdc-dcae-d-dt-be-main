package org.onap.sdc.dcae.rule.editor.utils;

import java.util.List;

import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class RulesPayloadUtils {
	private static Gson gson = new GsonBuilder().serializeNulls()
			.registerTypeAdapter(BaseAction.class, new ActionDeserializer())
			.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

	public static Rule parsePayloadToRule(String payload) throws JsonParseException {
		return gson.fromJson(payload, Rule.class);
	}

	public static MappingRules parseMappingRulesArtifactPayload(String payload) throws JsonParseException {
		return gson.fromJson(payload, MappingRules.class);
	}

	public static SchemaInfo extractInfoFromDescription(Artifact rulesArtifact) {
		try {
			return gson.fromJson(rulesArtifact.getArtifactDescription(), SchemaInfo.class);
		}catch (JsonParseException e) {
			return null;
		}
	}

	public static String buildSchemaAndRulesResponse(String payload, List<EventTypeDefinitionUI> schema) {
		return  "{\"schema\":"+gson.toJson(schema)+","+payload.replaceFirst("\\{", "");
	}


}
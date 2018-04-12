package org.onap.sdc.dcae.rule.editor.utils;

import com.google.gson.*;

import java.lang.reflect.Type;

public class EmptyStringTranslationSerializer implements JsonSerializer<String> {

	public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
		if("\"\"".equals(src))
			return new JsonPrimitive("");
		return new JsonPrimitive(src);
	}
}

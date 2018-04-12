package org.onap.sdc.dcae.ves;

import com.google.gson.*;

import java.lang.reflect.Type;

// json 'items' value can be either a single object or an array. customized POJO will always be an array
public class VesJsonDeserializer implements JsonDeserializer<VesDataItemsDefinition> {
	@Override
	public VesDataItemsDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		if(json instanceof JsonArray){
			 return new Gson().fromJson(json, VesDataItemsDefinition.class);
		}

		VesDataItemsDefinition items = new VesDataItemsDefinition();
		items.add(new Gson().fromJson(json, VesDataTypeDefinition.class));
		return items;
	}
}

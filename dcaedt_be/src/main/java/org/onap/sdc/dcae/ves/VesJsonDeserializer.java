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

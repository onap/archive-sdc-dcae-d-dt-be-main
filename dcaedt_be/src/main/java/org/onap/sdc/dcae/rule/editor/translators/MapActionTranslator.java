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

package org.onap.sdc.dcae.rule.editor.translators;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MapAction;

public class MapActionTranslator extends ActionTranslator<MapAction> {

	private static MapActionTranslator mapActionTranslator = new MapActionTranslator();

	public static MapActionTranslator getInstance() {
		return mapActionTranslator;
	}

	private MapActionTranslator(){}

	private class MapActionTranslation extends ProcessorTranslation {

		private Map<String, String> map;
		private String field;
		private String toField;
		@SerializedName("default")
		private String Default;

		private MapActionTranslation(MapAction action) {
			clazz = "MapAlarmValues";
			Default = action.getMapDefaultValue();
			field = action.fromValue();
			toField = action.getTarget();
			map = action.transformToMap();
		}
	}

	public Object translateToHpJson(MapAction action) {
		return new MapActionTranslation(action);
	}

}

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

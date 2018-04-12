package org.onap.sdc.dcae.rule.editor.translators;

import com.google.gson.annotations.SerializedName;

import org.onap.sdc.common.onaplog.Enums.LogLevel;

import java.util.List;
import java.util.Map;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MapAction;

public class MapActionTranslator extends CopyActionTranslator<MapAction> {

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
			field = action.getFromValue();
			toField = action.getTarget();
			map = action.transformToMap();
		}
	}

	@Override
	public Translation translateToHpJson(MapAction action) {
		return new MapActionTranslation(action);
	}

	@Override
	public boolean addToHpJsonProcessors(MapAction action, List<Translation> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating map action");
		addToHpJsonProcessors(action, processors);
		return true;
	}
}

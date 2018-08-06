package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class ActionTranslator<A extends BaseAction> implements IRuleElementTranslator<A> {

	boolean addToHpJsonProcessors(A action, List<Object> processors, boolean asNewProcessor){
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating {} action", action.getActionType());
		processors.add(translateToHpJson(action));
		return true;
	}

	class CopyActionSetTranslation extends ProcessorTranslation {
		protected Map<String, String> updates = new LinkedHashMap<>();

		CopyActionSetTranslation(String target, String from) {
			clazz = "Set";
			updates.put(target, from);
		}
	}
}

package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CopyActionTranslator extends ActionTranslator<BaseCopyAction> {

	private static CopyActionTranslator copyActionTranslator = new CopyActionTranslator();

	public static CopyActionTranslator getInstance() {
		return copyActionTranslator;
	}

	CopyActionTranslator(){}

	public Object translateToHpJson(BaseCopyAction action) {
		return new CopyActionSetTranslation(action.getTarget(), action.getFromValue());
	}

	@Override
	public boolean addToHpJsonProcessors(BaseCopyAction action, List<Object> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating {} action. New Processor: {}", action.getActionType(), asNewProcessor);
		if(asNewProcessor) {
			processors.add(translateToHpJson(action));
		}
		else {
			((CopyActionSetTranslation) processors.get(processors.size() - 1)).updates.put(action.getTarget(), action.getFromValue());
		}
		return false;
	}

	class CopyActionSetTranslation extends ProcessorTranslation {
		Map<String, String> updates = new LinkedHashMap<>();
		CopyActionSetTranslation(String target, String from) {
			clazz = "Set";
			updates.put(target, from);
		}
	}

}

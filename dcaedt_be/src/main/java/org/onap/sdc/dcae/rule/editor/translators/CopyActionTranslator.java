package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;

public class CopyActionTranslator<A extends BaseAction> implements IRuleElementTranslator<A>{

	private static CopyActionTranslator copyActionTranslator = new CopyActionTranslator();

	public static CopyActionTranslator getInstance() {
		return copyActionTranslator;
	}

	CopyActionTranslator(){}

	public Translation translateToHpJson(A action) {
		return new CopyActionSetTranslation(action.getTarget(), action.getFromValue());
	}

	void addToHpJsonProcessors(A action, List<Translation> processors) {
		processors.add(translateToHpJson(action));
	}

	public boolean addToHpJsonProcessors(A action, List<Translation> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating {} action. New Processor: {}", action.getActionType(), asNewProcessor);
		if(asNewProcessor)
			addToHpJsonProcessors(action, processors);
		else
			((CopyActionSetTranslation) processors.get(processors.size()-1)).updates.put(action.getTarget(), action.getFromValue());
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

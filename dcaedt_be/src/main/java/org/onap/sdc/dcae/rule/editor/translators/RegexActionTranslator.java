package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import java.util.List;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;

public class RegexActionTranslator extends CopyActionTranslator<BaseAction> {

	private static RegexActionTranslator regexActionTranslator = new RegexActionTranslator();

	public static RegexActionTranslator getInstance() {
		return regexActionTranslator;
	}

	private RegexActionTranslator(){}

	private class RegexCopyActionTranslation extends ProcessorTranslation {

		private String regex;
		private String field;
		private String value;

		private RegexCopyActionTranslation(BaseAction action) {
			clazz = "ExtractText";
			regex = action.getRegexValue();
			field = action.getTarget();
			value = action.getFromValue();
		}
	}

	@Override
	public boolean addToHpJsonProcessors(BaseAction action, List<Translation> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating copy action as regex action");
		addToHpJsonProcessors(action, processors);
		return true;
	}

	@Override
	public Translation translateToHpJson(BaseAction action) {
		return new RegexCopyActionTranslation(action);
	}

}

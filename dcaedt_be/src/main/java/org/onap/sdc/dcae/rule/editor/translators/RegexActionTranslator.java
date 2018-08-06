package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;

public class RegexActionTranslator extends ActionTranslator<BaseCopyAction> {

	private static RegexActionTranslator regexActionTranslator = new RegexActionTranslator();

	public static RegexActionTranslator getInstance() {
		return regexActionTranslator;
	}

	private RegexActionTranslator(){}

	private class RegexCopyActionTranslation extends ProcessorTranslation {

		private String regex;
		private String field;
		private String value;

		private RegexCopyActionTranslation(BaseCopyAction action) {
			clazz = "ExtractText";
			regex = action.regexValue();
			field = action.getTarget();
			value = action.fromValue();
		}
	}


	public Object translateToHpJson(BaseCopyAction action) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating copy action as regex action");
		return new RegexCopyActionTranslation(action);
	}

}

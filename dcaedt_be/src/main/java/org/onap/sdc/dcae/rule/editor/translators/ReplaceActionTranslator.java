package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ReplaceTextAction;

public class ReplaceActionTranslator extends ActionTranslator<ReplaceTextAction> {

	private static ReplaceActionTranslator replaceActionTranslator = new ReplaceActionTranslator();

	public static ReplaceActionTranslator getInstance() {
		return replaceActionTranslator;
	}

	private ReplaceActionTranslator(){}

	public Object translateToHpJson(ReplaceTextAction action) {
		return new ReplaceActionTranslation(action);
	}


	private class ReplaceActionTranslation extends ProcessorTranslation {
		private String field;
		private String find;
		private String replace;

		ReplaceActionTranslation(ReplaceTextAction action) {
			clazz = "ReplaceText";
			field = action.getFromValue();
			find = action.getFind();
			replace = action.getReplace();
		}
	}

}

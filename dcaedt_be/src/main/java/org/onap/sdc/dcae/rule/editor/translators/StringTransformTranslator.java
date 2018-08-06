package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.StringTransformAction;

public class StringTransformTranslator extends ActionTranslator<StringTransformAction> {

	private static StringTransformTranslator stringTransformTranslator = new StringTransformTranslator();

	public static StringTransformTranslator getInstance() {
		return stringTransformTranslator;
	}

	private StringTransformTranslator() {
	}

	private class StringTransformTranslation extends ProcessorTranslation {
		private String targetCase;
		private String trim;
		private String toField;
		private String value;

		private StringTransformTranslation(StringTransformAction action) {
			clazz = "StringTransform";
			targetCase = action.targetCase();
			trim = String.valueOf(action.trim());
			toField = action.getTarget();
			value = action.fromValue();
		}
	}

	public Object translateToHpJson(StringTransformAction action) {
		return new StringTransformTranslation(action);
	}
}
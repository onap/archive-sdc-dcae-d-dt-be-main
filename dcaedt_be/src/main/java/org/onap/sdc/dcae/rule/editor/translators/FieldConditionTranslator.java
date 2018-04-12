package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;

import java.util.List;

public class FieldConditionTranslator implements IRuleElementTranslator<Condition> {

	private static FieldConditionTranslator fieldConditionTranslator = new FieldConditionTranslator();

	public static FieldConditionTranslator getInstance() {
		return fieldConditionTranslator;
	}

	private FieldConditionTranslator(){}

	private class FieldFilterTranslation extends ProcessorTranslation {
		private String field;
		private String value;

		private FieldFilterTranslation(Condition condition) {
			clazz = OperatorTypeEnum.getTypeByName(condition.getOperator()).getType();
			field = condition.getLeft();
			value = condition.getRight().get(0);
		}
	}

	private class MultiFieldFilterTranslation extends ProcessorTranslation {
		private String field;
		private List<String> values;

		private MultiFieldFilterTranslation(Condition condition) {
			field = condition.getLeft();
			values = condition.getRight();
			clazz = OperatorTypeEnum.getTypeByName(condition.getOperator()).getModifiedType();
		}
	}

	public Translation translateToHpJson(Condition condition) {
		return 1 == condition.getRight().size() ? new FieldFilterTranslation(condition) : new MultiFieldFilterTranslation(condition);
	}
}

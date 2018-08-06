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

		private FieldFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			clazz = operatorType.getType();
			field = condition.getLeft();
			value = condition.getRight().get(0);
		}
	}

	private class UnaryFilterTranslation extends ProcessorTranslation {
		private String field;
		private boolean emptyIsAssigned;

		private UnaryFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			clazz = operatorType.getType();
			field = condition.getLeft();
			emptyIsAssigned = condition.isEmptyIsAssigned();
		}
	}

	private class MultiFieldFilterTranslation extends ProcessorTranslation {
		private String field;
		private List<String> values;

		private MultiFieldFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			field = condition.getLeft();
			values = condition.getRight();
			clazz = operatorType.getModifiedType().getType();
		}
	}

	public Object translateToHpJson(Condition condition) {
		OperatorTypeEnum operatorType = OperatorTypeEnum.getTypeByName(condition.getOperator());
		if(OperatorTypeEnum.UNASSIGNED == operatorType || OperatorTypeEnum.ASSIGNED == operatorType) {
			return new UnaryFilterTranslation(condition, operatorType);
		}
		return 1 == condition.getRight().size() && !alwaysUseMultipleRightValues(operatorType)? new FieldFilterTranslation(condition, operatorType) : new MultiFieldFilterTranslation(condition, operatorType);
	}

	private boolean alwaysUseMultipleRightValues(OperatorTypeEnum operatorType) {
		return operatorType.equals(operatorType.getModifiedType());
	}

}

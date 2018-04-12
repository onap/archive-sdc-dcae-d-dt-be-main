package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.rule.editor.enums.ConditionTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;

import java.util.stream.Collectors;

public class ConditionTranslator implements IRuleElementTranslator<Condition> {

	private static ConditionTranslator conditionTranslator = new ConditionTranslator();

	public static ConditionTranslator getInstance() {
		return conditionTranslator;
	}

	private ConditionTranslator(){}

	private class StringFilterTranslation extends ProcessorTranslation {
		private String string;
		private String value;

		private StringFilterTranslation(Condition condition, String value){
			this.clazz = OperatorTypeEnum.getTypeByName(condition.getOperator()).getType();
			this.string = condition.getLeft();
			this.value = value;
		}

		private StringFilterTranslation(Condition condition){
			this(condition, condition.getRight().get(0));
		}
	}

	public Translation translateToHpJson(Condition condition) {
		return 1 == condition.getRight().size() ? new StringFilterTranslation(condition) : new FiltersTranslation(ConditionTypeEnum.ANY.getFilterClass(), condition.getRight().stream()
				.map(r -> new StringFilterTranslation(condition, r)).collect(Collectors.toList()));
	}


}

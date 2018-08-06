package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCondition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ConditionGroup;
import org.onap.sdc.dcae.rule.editor.enums.ConditionTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionGroupTranslator implements IRuleElementTranslator<ConditionGroup> {

	private static ConditionGroupTranslator conditionGroupTranslator = new ConditionGroupTranslator();

	public static ConditionGroupTranslator getInstance() {
		return conditionGroupTranslator;
	}

	private ConditionGroupTranslator(){}

	public Object translateToHpJson(ConditionGroup conditionGroup) {
		String clazz = ConditionTypeEnum.getTypeByName(conditionGroup.getType()).getFilterClass();
		FiltersTranslation translation = new FiltersTranslation(clazz, conditionGroup.getChildren().stream()
				.map(this::getTranslation)
				.collect(Collectors.toList()));
		flattenNestedFilters(translation, clazz);
		return translation;
	}


	private IRuleElementTranslator getConditionTranslator(BaseCondition condition){
		return condition instanceof ConditionGroup ? ConditionGroupTranslator.getInstance() : getSimpleConditionTranslator((Condition) condition);
	}

	private IRuleElementTranslator getSimpleConditionTranslator(Condition condition) {
		String conditionType = OperatorTypeEnum.getTypeByName(condition.getOperator()).getConditionType();
		return RuleEditorElementType.getElementTypeByName(conditionType).getTranslator();
	}

	private Object getTranslation(BaseCondition condition) {
		return getConditionTranslator(condition).translateToHpJson(condition);
	}

	private void flattenNestedFilters(FiltersTranslation filtersTranslation, String clazz) {
		Map<Boolean, List<Object>> partitioned = filtersTranslation.filters.stream().collect(Collectors.partitioningBy(f -> clazz.equals(((ProcessorTranslation) f).clazz)));
		filtersTranslation.filters.removeAll(partitioned.get(Boolean.TRUE));
		filtersTranslation.filters.addAll(partitioned.get(Boolean.TRUE).stream().map(f -> ((FiltersTranslation) f).filters).flatMap(List::stream).collect(Collectors.toList()));
	}

}

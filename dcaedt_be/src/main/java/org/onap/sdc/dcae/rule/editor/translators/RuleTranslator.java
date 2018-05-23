package org.onap.sdc.dcae.rule.editor.translators;

import com.google.gson.Gson;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

public class RuleTranslator implements IRuleElementTranslator<Rule> {

	private static RuleTranslator ruleTranslator = new RuleTranslator();

	public static RuleTranslator getInstance() {
		return ruleTranslator;
	}

	private RuleTranslator() {
	}

	private class ActionRuleTranslation extends RuleTranslation {
		private ActionRuleTranslation(Rule rule) {
			phase = rule.getPhase();
			filter = rule.isConditionalRule() ? getConditionTranslator(rule.getCondition()).translateToHpJson(rule.getCondition()) : null;
			boolean asNewProcessor = true;
			for (BaseAction action : rule.getActions()) {
				// consecutive copy actions are aggregated into a single processor
				asNewProcessor = getActionTranslator(action).addToHpJsonProcessors(action, processors, asNewProcessor);
			}
		}
	}

	public Object translateToHpJson(Rule rule) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Start translating rule {}", rule.getUid());
		Object translation = new ActionRuleTranslation(rule);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished translation for rule {}. Result: {}", rule.getUid(), new Gson().toJson(translation));
		return translation;
	}

	private IRuleElementTranslator getConditionTranslator(BaseCondition condition){
		return condition instanceof ConditionGroup ? ConditionGroupTranslator.getInstance() :
				ValidationUtils.validateNotEmpty(OperatorTypeEnum.getTypeByName(((Condition)condition).getOperator()).getModifiedType()) ? FieldConditionTranslator.getInstance() : ConditionTranslator.getInstance();
	}

	private ActionTranslator getActionTranslator(BaseAction action) {
		ActionTypeEnum type = ActionTypeEnum.getTypeByName(action.getActionType());
		if(ActionTypeEnum.COPY == type && ValidationUtils.validateNotEmpty(((BaseCopyAction)action).getRegexValue())) {
			return RegexActionTranslator.getInstance();
		}
		return (ActionTranslator) RuleEditorElementType.getElementTypeByName(type.getType()).getTranslator();
	}
}
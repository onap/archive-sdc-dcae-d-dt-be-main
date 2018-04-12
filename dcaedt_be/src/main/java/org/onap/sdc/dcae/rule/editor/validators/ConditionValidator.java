package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ConditionValidator implements IRuleElementValidator<Condition> {

	private static ConditionValidator conditionValidator = new ConditionValidator();

	public static ConditionValidator getInstance() {
		return conditionValidator;
	}

	private ConditionValidator(){}

	public boolean validate(Condition condition, List<ResponseFormat> errors) {
		boolean valid = true;
		if(!ValidationUtils.validateNotEmpty(condition.getLeft())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "left"));
		}
		if(CollectionUtils.isEmpty(condition.getRight())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "right"));
		}
		if(!ValidationUtils.validateNotEmpty(condition.getOperator()) || null == OperatorTypeEnum.getTypeByName(condition.getOperator())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_OPERATOR, null, condition.getOperator()));
		}
		return valid;
	}

}

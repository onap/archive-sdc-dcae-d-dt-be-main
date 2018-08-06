package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ConditionValidator extends BaseConditionValidator<Condition> {

	private static ConditionValidator conditionValidator = new ConditionValidator();

	public static ConditionValidator getInstance() {
		return conditionValidator;
	}

	private ConditionValidator(){}

	@Override
	public boolean validate(Condition condition, List<ResponseFormat> errors) {
		return validateConditionalAction(condition, errors) && super.validate(condition, errors);
	}


	public boolean validateConditionalAction(Condition condition, List<ResponseFormat> errors) {
		boolean valid = true;
		if(!ValidationUtils.validateNotEmpty(condition.getLeft())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "left"));
		}
		OperatorTypeEnum operatorTypeEnum = OperatorTypeEnum.getTypeByName(condition.getOperator());
		if(null == operatorTypeEnum) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_OPERATOR, null, condition.getOperator()));
		}
		if(OperatorTypeEnum.ASSIGNED != operatorTypeEnum && OperatorTypeEnum.UNASSIGNED != operatorTypeEnum && (condition.getRight().isEmpty() || !condition.getRight().stream().allMatch(ValidationUtils::validateNotEmpty))) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "right"));
		}
		return valid;
	}

}

package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCondition;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public abstract class BaseConditionValidator<C extends BaseCondition> implements IRuleElementValidator<C> {

	public boolean validate(C condition, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(condition.getLevel()) || !ValidationUtils.validateNotEmpty(condition.getId()) || !ValidationUtils.validateNotEmpty(condition.getName())) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_RULE_FORMAT, "", "missing condition information"));
			return false;
		}
		return true;
	}
}

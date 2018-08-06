package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public abstract class BaseActionValidator<A extends BaseAction> implements IRuleElementValidator<A> {

	public boolean validate(A action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getId())) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_RULE_FORMAT, "", "missing action id"));
			return false;
		}
		return true;
	}
}

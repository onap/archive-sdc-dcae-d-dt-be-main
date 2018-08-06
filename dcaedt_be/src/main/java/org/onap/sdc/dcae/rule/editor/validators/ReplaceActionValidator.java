package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ReplaceTextAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class ReplaceActionValidator extends BaseActionValidator<ReplaceTextAction> {

	private static ReplaceActionValidator replaceActionValidator = new ReplaceActionValidator();

	public static ReplaceActionValidator getInstance() {
		return replaceActionValidator;
	}

	private ReplaceActionValidator(){}

	public boolean validate(ReplaceTextAction action, List<ResponseFormat> errors) {
		boolean valid = super.validate(action, errors);
		if(!ValidationUtils.validateNotEmpty(action.fromValue())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from", action.getActionType(), action.strippedTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.find())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "find", action.getActionType(), action.strippedTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.replace())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "replace", action.getActionType(), action.strippedTarget()));
		}
		return valid;
	}
}

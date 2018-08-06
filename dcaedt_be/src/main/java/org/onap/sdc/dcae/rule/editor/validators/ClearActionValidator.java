package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.UnaryFieldAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class ClearActionValidator extends BaseActionValidator<UnaryFieldAction> {

	private static ClearActionValidator clearActionValidator = new ClearActionValidator();

	public static ClearActionValidator getInstance() {
		return clearActionValidator;
	}

	private ClearActionValidator(){}

	@Override
	public boolean validate(UnaryFieldAction action, List<ResponseFormat> errors) {
		if(action.fromValues().isEmpty() || !action.fromValues().stream().allMatch(ValidationUtils::validateNotEmpty)) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return super.validate(action, errors);
	}
}

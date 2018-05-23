package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class CopyActionValidator<A extends BaseCopyAction> implements IRuleElementValidator<A> {

	private static CopyActionValidator copyActionValidator = new CopyActionValidator();

	public static CopyActionValidator getInstance() {
		return copyActionValidator;
	}

	CopyActionValidator(){}

	public boolean validate(A action, List<ResponseFormat> errors) {

		// validate from is populated
		boolean valid = validateFromValue(action, errors);
		//validate target is populated
		if (!ValidationUtils.validateTargetField(action.getTarget())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "target", action.getActionType(), action.getTarget()));
		}
		return valid;
	}

	protected boolean validateFromValue(A action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getFromValue())) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from", action.getActionType(), action.getTarget()));
			return false;
		}
		return true;
	}
}

package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.UnaryFieldAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;

import java.util.List;

public class ClearActionValidator implements IRuleElementValidator<UnaryFieldAction> {

	private static ClearActionValidator clearActionValidator = new ClearActionValidator();

	public static ClearActionValidator getInstance() {
		return clearActionValidator;
	}

	private ClearActionValidator(){}

	public boolean validate(UnaryFieldAction action, List<ResponseFormat> errors) {
		if(action.getFromValues().isEmpty()) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return true;
	}
}

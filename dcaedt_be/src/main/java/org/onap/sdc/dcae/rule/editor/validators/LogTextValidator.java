package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.LogTextAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class LogTextValidator implements IRuleElementValidator<LogTextAction> {

	private static LogTextValidator logTextValidator = new LogTextValidator();

	public static LogTextValidator getInstance() {
		return logTextValidator;
	}

	LogTextValidator(){}

	public boolean validate(LogTextAction action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getText())){
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "text", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return true;
	}
}

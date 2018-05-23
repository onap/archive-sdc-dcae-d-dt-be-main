package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.LogEventAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class LogEventValidator implements IRuleElementValidator<LogEventAction> {

	private static LogEventValidator logEventValidator = new LogEventValidator();

	public static LogEventValidator getInstance() {
		return logEventValidator;
	}

	LogEventValidator(){}

	public boolean validate(LogEventAction action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getTitle())){
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "title", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return true;
	}
}

package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.DateFormatterAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class DateFormatterValidator extends ActionValidator<DateFormatterAction> {
	private static DateFormatterValidator dateFormatterValidator = new DateFormatterValidator();

	public static DateFormatterValidator getInstance() {
		return dateFormatterValidator;
	}

	private DateFormatterValidator(){}

	@Override
	public boolean validate(DateFormatterAction action, List<ResponseFormat> errors) {
		boolean valid = super.validate(action, errors);
		if(!ValidationUtils.validateNotEmpty(action.getFromFormat())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from format", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.getFromTz())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from timezone", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.getToFormat())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "to format", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.getToTz())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "to timezone", action.getActionType(), action.getTarget()));
		}
		return valid;
	}
}

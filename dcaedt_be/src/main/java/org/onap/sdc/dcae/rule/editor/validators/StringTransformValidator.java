package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.StringTransformAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class StringTransformValidator extends BaseActionValidator<StringTransformAction> {

	private static StringTransformValidator stringTransformValidator = new StringTransformValidator();

	public static StringTransformValidator getInstance() {
		return stringTransformValidator;
	}

	private StringTransformValidator(){}

	@Override
	public boolean validate(StringTransformAction action, List<ResponseFormat> errors) {
		boolean valid = super.validate(action, errors);
		if(!ValidationUtils.validateNotEmpty(action.targetCase())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "target case", action.getActionType(), action.getTarget()));
		}
		if (!ValidationUtils.validateTargetField(action.getTarget())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "target", action.getActionType(), action.getTarget()));
		}
		if (!ValidationUtils.validateTargetField(action.startValue())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "value", action.getActionType(), action.getTarget()));
		}
		return valid;
	}
}

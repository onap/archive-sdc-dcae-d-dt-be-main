package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class ConcatActionValidator extends CopyActionValidator<BaseCopyAction> {

	private static ConcatActionValidator concatActionValidator = new ConcatActionValidator();

	public static ConcatActionValidator getInstance() {
		return concatActionValidator;
	}

	private ConcatActionValidator(){}

	@Override
	protected boolean validateFromValue(BaseCopyAction action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getFromValue()) || 2 > action.getFromValues().size()) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_CONCAT_VALUE, null, action.getTarget()));
			return false;
		}
		return true;
	}
}

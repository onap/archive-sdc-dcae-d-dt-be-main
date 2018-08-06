package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.HpMetricAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class HpMetricValidator extends BaseActionValidator<HpMetricAction> {

	private static HpMetricValidator hpMetricValidator = new HpMetricValidator();

	public static HpMetricValidator getInstance() {
		return hpMetricValidator;
	}

	private HpMetricValidator(){}

	@Override
	public boolean validate(HpMetricAction action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.getSelectedHpMetric())){
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "HP Metric", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return super.validate(action, errors);
	}
}
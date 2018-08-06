package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.TopoSearchAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class TopoSearchValidator extends BaseActionValidator<TopoSearchAction> {

	private static TopoSearchValidator topoSearchValidator = new TopoSearchValidator();

	public static TopoSearchValidator getInstance() {
		return topoSearchValidator;
	}

	private TopoSearchValidator() {
	}

	private ConditionValidator conditionValidator = ConditionValidator.getInstance();

	@Override
	public boolean validate(TopoSearchAction action, List<ResponseFormat> errors) {

		boolean valid = super.validate(action, errors);
		if (action.conditionalSearch() && !conditionValidator.validateConditionalAction(action.searchFilter(), errors)) {
			valid = false;
		}
		if (!ValidationUtils.validateNotEmpty(action.searchField())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "search field", action.getActionType(), action.strippedTarget()));
		}
		if (!ValidationUtils.validateNotEmpty(action.searchValue())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "search value", action.getActionType(), action.strippedTarget()));
		}
		return validateEnrichOrUpdates(action, errors) && valid;
	}


	private boolean validateEnrichOrUpdates(TopoSearchAction action, List<ResponseFormat> errors) {
		if (!action.doEnrich()) {
			return validateUpdatesMap(action, errors);
		}
		if (action.enrichFields().isEmpty() || !action.enrichFields().stream().allMatch(ValidationUtils::validateNotEmpty)) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "enrich fields", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return true;
	}

	private boolean validateUpdatesMap(TopoSearchAction action, List<ResponseFormat> errors) {
		boolean valid = true;
		try {
			if (!action.updatesMap().entrySet().stream().allMatch(p -> ValidationUtils.validateNotEmpty(p.getKey()) && ValidationUtils.validateNotEmpty(p.getValue()))) {
				valid = false;
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "key/value", action.getActionType(), action.strippedTarget()));
			}
		} catch (IllegalStateException err) {
			valid = false;
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "updates validation error: {}", err);
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.DUPLICATE_KEY, null));
		}
		return valid;
	}
}

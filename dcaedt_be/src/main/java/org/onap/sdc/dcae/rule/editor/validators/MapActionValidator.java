package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MapAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class MapActionValidator extends CopyActionValidator<MapAction> {

	private static MapActionValidator mapActionValidator = new MapActionValidator();

	public static MapActionValidator getInstance() {
		return mapActionValidator;
	}

	private MapActionValidator(){}

	@Override
	public boolean validate(MapAction action, List<ResponseFormat> errors) {
		boolean valid = super.validate(action, errors);
		if (action.getMap() == null || CollectionUtils.isEmpty(action.getMapValues())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ENTRY, null, action.getTarget()));
		} else {
			if (action.mapHasDefault() && !ValidationUtils.validateNotEmpty(action.getMapDefaultValue())) {
				valid = false;
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_DEFAULT_VALUE, null, action.getTarget()));
			}
			try {
				if (!validateMapValues(action)) {
					valid = false;
					errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ENTRY, null, action.getTarget()));
				}
			} catch (IllegalStateException err) {
				valid = false;
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Map validation error: {}", err);
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.DUPLICATE_KEY, null));
			}
		}
		return valid;
	}

	private boolean validateMapValues(MapAction action) {
		return action.transformToMap().entrySet().stream().allMatch(p -> ValidationUtils.validateNotEmpty(p.getKey()) && ValidationUtils.validateNotEmpty(p.getValue()));
	}
}

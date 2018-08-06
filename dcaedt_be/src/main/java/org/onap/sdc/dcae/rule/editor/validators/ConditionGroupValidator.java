package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ConditionGroup;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.enums.ConditionTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ConditionGroupValidator extends BaseConditionValidator<ConditionGroup> {

	private static ConditionGroupValidator conditionGroupValidator = new ConditionGroupValidator();

	public static ConditionGroupValidator getInstance() {
		return conditionGroupValidator;
	}

	private ConditionGroupValidator(){}

	@Override
	public boolean validate(ConditionGroup condition, List<ResponseFormat> errors) {
		boolean valid = super.validate(condition, errors);
		if(!ValidationUtils.validateNotEmpty(condition.getType()) || null == ConditionTypeEnum.getTypeByName(condition.getType())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_GROUP_CONDITION, null, condition.getType()));
		}
		if(CollectionUtils.isEmpty(condition.getChildren()) || 2 > condition.getChildren().size()) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_CONDITION_ITEM, null, null));
		} else {
			valid = condition.getChildren().stream()
					.map(c -> RuleEditorElementType.getElementTypeByName(c.getClass().getSimpleName()).getValidator().validate(c, errors))
					.reduce(true, (x,y) -> x && y) && valid;
		}
		return valid;
	}
}

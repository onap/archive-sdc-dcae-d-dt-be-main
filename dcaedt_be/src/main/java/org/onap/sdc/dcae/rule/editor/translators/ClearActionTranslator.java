package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ActionTypeEnum;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.UnaryFieldAction;

import java.util.List;

public class ClearActionTranslator extends ActionTranslator<UnaryFieldAction> {

	private static ClearActionTranslator clearActionTranslator = new ClearActionTranslator();

	public static ClearActionTranslator getInstance() {
		return clearActionTranslator;
	}

	private ClearActionTranslator(){}

	public Object translateToHpJson(UnaryFieldAction action) {
		return ActionTypeEnum.CLEAR == ActionTypeEnum.getTypeByName(action.getActionType()) ? new ClearActionTranslation(action) : new ClearNSFActionTranslation(action);
	}


	private class ClearActionTranslation extends ProcessorTranslation {
		private List<String> fields;

		ClearActionTranslation(UnaryFieldAction action) {
			clazz = "Clear";
			fields = action.fromValues();
		}
	}


	private class ClearNSFActionTranslation extends ProcessorTranslation {
		private List<String> reservedFields;

		ClearNSFActionTranslation(UnaryFieldAction action) {
			clazz = "ClearNoneStandardFields";
			reservedFields = action.fromValues();
		}
	}
}

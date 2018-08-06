package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.HpMetricAction;

public class HpMetricTranslator extends ActionTranslator<HpMetricAction> {

	private static HpMetricTranslator hpMetricTranslator = new HpMetricTranslator();

	public static HpMetricTranslator getInstance() {
		return hpMetricTranslator;
	}

	private HpMetricTranslator(){}

	public Object translateToHpJson(HpMetricAction action) {
		return new CopyActionSetTranslation("parserType", action.getSelectedHpMetric());
	}
}

package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import java.util.List;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.DateFormatterAction;

public class DateFormatterTranslator extends CopyActionTranslator<DateFormatterAction> {

	private static DateFormatterTranslator dateFormatterTranslator = new DateFormatterTranslator();

	public static DateFormatterTranslator getInstance() {
		return dateFormatterTranslator;
	}

	private DateFormatterTranslator(){}

	private class DateFormatterTranslation extends ProcessorTranslation {
		private String fromFormat;
		private String fromTz;
		private String toField;
		private String toFormat;
		private String toTz;
		private String value;

		private DateFormatterTranslation(DateFormatterAction action){
			clazz = "DateFormatter";
			fromFormat = action.getFromFormat();
			fromTz = action.getFromTz();
			toField = action.getTarget();
			toFormat = action.getToFormat();
			toTz = action.getToTz();
			value = action.getFromValue();
		}
	}

	@Override
	public boolean addToHpJsonProcessors(DateFormatterAction action, List<Translation> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating date formatter action");
		addToHpJsonProcessors(action, processors);
		return true;
	}

	@Override
	public Translation translateToHpJson(DateFormatterAction action){
		return new DateFormatterTranslation(action);
	}

}

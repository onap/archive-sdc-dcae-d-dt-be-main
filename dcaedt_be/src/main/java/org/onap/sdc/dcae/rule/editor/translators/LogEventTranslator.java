package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.LogEventAction;

public class LogEventTranslator extends ActionTranslator<LogEventAction> {

	private static LogEventTranslator logEventTranslator = new LogEventTranslator();

	public static LogEventTranslator getInstance() {
		return logEventTranslator;
	}

	private LogEventTranslator(){}

	public Object translateToHpJson(LogEventAction action) {
		return new LogEventTranslation(action);
	}


	private class LogEventTranslation extends ProcessorTranslation {
		String title;

		LogEventTranslation(LogEventAction action) {
			clazz = "LogEvent";
			title = action.getTitle();
		}
	}
}

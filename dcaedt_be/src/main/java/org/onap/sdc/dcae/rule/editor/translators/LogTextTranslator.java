package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.LogTextAction;

public class LogTextTranslator extends ActionTranslator<LogTextAction> {

	private static LogTextTranslator logTextTranslator = new LogTextTranslator();

	public static LogTextTranslator getInstance() {
		return logTextTranslator;
	}

	private LogTextTranslator(){}

	public Object translateToHpJson(LogTextAction action) {
		return new LogTextTranslation(action);
	}


	class LogTextTranslation extends ProcessorTranslation {
		private String logLevel;
		private String logName;
		private String logText;

		private LogTextTranslation(LogTextAction action) {
			clazz = "LogText";
			logLevel = action.getLevel();
			logName = action.getName();
			logText = action.getText();
		}
	}

}

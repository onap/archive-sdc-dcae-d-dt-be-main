/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

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
			logLevel = action.logLevel();
			logName = action.logName();
			logText = action.logText();
		}
	}

}

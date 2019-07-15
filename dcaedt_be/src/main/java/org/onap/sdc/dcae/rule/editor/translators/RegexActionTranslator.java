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

import org.onap.sdc.common.onaplog.enums.LogLevel;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;

public class RegexActionTranslator extends ActionTranslator<BaseCopyAction> {

	private static RegexActionTranslator regexActionTranslator = new RegexActionTranslator();

	public static RegexActionTranslator getInstance() {
		return regexActionTranslator;
	}

	private RegexActionTranslator(){}

	private class RegexCopyActionTranslation extends ProcessorTranslation {

		private String regex;
		private String field;
		private String value;

		private RegexCopyActionTranslation(BaseCopyAction action) {
			clazz = "ExtractText";
			regex = action.regexValue();
			field = action.getTarget();
			value = action.fromValue();
		}
	}


	public Object translateToHpJson(BaseCopyAction action) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating copy action as regex action");
		return new RegexCopyActionTranslation(action);
	}

}

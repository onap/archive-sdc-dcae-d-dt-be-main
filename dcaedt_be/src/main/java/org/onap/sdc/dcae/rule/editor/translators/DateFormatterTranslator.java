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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.DateFormatterAction;

public class DateFormatterTranslator extends ActionTranslator<DateFormatterAction> {

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
			fromFormat = action.fromFormat();
			fromTz = action.fromTz();
			toField = action.getTarget();
			toFormat = action.toFormat();
			toTz = action.toTz();
			value = action.fromValue();
		}
	}

	public Object translateToHpJson(DateFormatterAction action){
		return new DateFormatterTranslation(action);
	}

}

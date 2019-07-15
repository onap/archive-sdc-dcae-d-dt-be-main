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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.StringTransformAction;

public class StringTransformTranslator extends ActionTranslator<StringTransformAction> {

	private static StringTransformTranslator stringTransformTranslator = new StringTransformTranslator();

	public static StringTransformTranslator getInstance() {
		return stringTransformTranslator;
	}

	private StringTransformTranslator() {
	}

	private class StringTransformTranslation extends ProcessorTranslation {
		private String targetCase;
		private String trim;
		private String toField;
		private String value;

		private StringTransformTranslation(StringTransformAction action) {
			clazz = "StringTransform";
			targetCase = action.targetCase();
			trim = String.valueOf(action.trim());
			toField = action.getTarget();
			value = action.startValue();
		}
	}

	public Object translateToHpJson(StringTransformAction action) {
		return new StringTransformTranslation(action);
	}
}

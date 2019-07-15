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

import java.util.List;

public class CopyActionTranslator extends ActionTranslator<BaseCopyAction> {

	private static CopyActionTranslator copyActionTranslator = new CopyActionTranslator();

	public static CopyActionTranslator getInstance() {
		return copyActionTranslator;
	}

	private CopyActionTranslator(){}

	public Object translateToHpJson(BaseCopyAction action) {
		return new CopyActionSetTranslation(action.getTarget(), action.fromValue());
	}

	@Override
	public boolean addToHpJsonProcessors(BaseCopyAction action, List<Object> processors, boolean asNewProcessor) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translating {} action. New Processor: {}", action.getActionType(), asNewProcessor);
		if(asNewProcessor) {
			processors.add(translateToHpJson(action));
		}
		else {
			((CopyActionSetTranslation) processors.get(processors.size() - 1)).updates.put(action.getTarget(), action.fromValue());
		}
		return false;
	}

}

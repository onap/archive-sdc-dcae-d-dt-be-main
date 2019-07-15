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

import com.google.gson.annotations.SerializedName;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;

import java.util.ArrayList;
import java.util.List;

public interface IRuleElementTranslator<T> {

	OnapLoggerError errLogger = OnapLoggerError.getInstance();
	OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	
	Object translateToHpJson(T element);


	class ProcessorTranslation {
		@SerializedName("class")
		protected String clazz;
	}


	class FiltersTranslation extends ProcessorTranslation {
		protected List<Object> filters;

		protected FiltersTranslation(String clazz, List<Object> filters) {
			this.clazz = clazz;
			this.filters = filters;
		}
	}

	class RuleTranslation {
		protected String phase;
		protected Object filter;
		protected List<ProcessorTranslation> processors = new ArrayList<>();
	}

	class RunPhaseProcessorsTranslation extends ProcessorTranslation {
		protected String phase;

		protected RunPhaseProcessorsTranslation(String runPhase){
			clazz ="RunPhase";
			phase = runPhase;
		}
	}

}

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
		protected List<Object> processors = new ArrayList<>();
	}

	class RunPhaseProcessorsTranslation extends ProcessorTranslation {
		protected String phase;

		protected RunPhaseProcessorsTranslation(String runPhase){
			clazz ="RunPhase";
			phase = runPhase;
		}
	}

}

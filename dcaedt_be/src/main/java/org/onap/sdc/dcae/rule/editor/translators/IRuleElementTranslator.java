package org.onap.sdc.dcae.rule.editor.translators;

import com.google.gson.annotations.SerializedName;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;

import java.util.ArrayList;
import java.util.List;

public interface IRuleElementTranslator<T> {

	OnapLoggerError errLogger = OnapLoggerError.getInstance();
	OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	
	Translation translateToHpJson(T element);

	abstract class Translation {
	}

	class ProcessorTranslation extends Translation {
		@SerializedName("class")
		protected String clazz;
	}


	class FiltersTranslation extends ProcessorTranslation {
		protected List<Translation> filters;

		protected FiltersTranslation(String clazz, List<Translation> filters) {
			this.clazz = clazz;
			this.filters = filters;
		}
	}

	class RuleTranslation extends Translation {
		protected String phase;
		protected Translation filter;
		protected List<Translation> processors = new ArrayList<>();
	}

	class RunPhaseProcessorsTranslation extends ProcessorTranslation {
		protected String phase;

		protected RunPhaseProcessorsTranslation(String runPhase){
			clazz ="RunPhase";
			phase = runPhase;
		}
	}

}

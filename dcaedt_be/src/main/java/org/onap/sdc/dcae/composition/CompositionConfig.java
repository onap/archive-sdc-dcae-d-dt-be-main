package org.onap.sdc.dcae.composition;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@PropertySources({
		@PropertySource(value="classpath:application-fe.properties", ignoreResourceNotFound=true),
		@PropertySource(value="file:${jetty.base}/config/dcae-be/application.properties", ignoreResourceNotFound=true)
})

public class CompositionConfig {

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	@Value("${compositionConfig.flowTypes}")
	private String flowTypes;
	@JsonIgnore
	private Map<String, FlowType> flowTypesMap;
	@Value("${compositionConfig.isRuleEditorActive}")
	private boolean isRuleEditorActive;

	// get flowTypes as the parsed keySet
	public Set<String> getFlowTypes() {
		return flowTypesMap.keySet();
	}

	@JsonProperty("isRuleEditorActive")
	public boolean isRuleEditorActive() {
		return isRuleEditorActive;
	}

	public Map<String, FlowType> getFlowTypesMap() {
		return flowTypesMap;
	}

	public static class FlowType {

		@JsonProperty("entryPhase")
		private String entryPointPhaseName;
		@JsonProperty("publishPhase")
		private String lastPhaseName;

		public String getEntryPointPhaseName() {
			return entryPointPhaseName;
		}

		public void setEntryPointPhaseName(String entryPointPhaseName) {
			this.entryPointPhaseName = entryPointPhaseName;
		}

		public String getLastPhaseName() {
			return lastPhaseName;
		}

		public void setLastPhaseName(String lastPhaseName) {
			this.lastPhaseName = lastPhaseName;
		}
	}


	@PostConstruct
	public void init() {
		try {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Reading flow type definitions from configuration");
			Type map = new TypeToken<Map<String, FlowType>>(){}.getType();
			flowTypesMap = new Gson().fromJson(flowTypes, map);
		} catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error – Failed to read flow type definitions. message: {}", e);
		}
	}
}
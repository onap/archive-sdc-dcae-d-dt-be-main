package org.onap.sdc.dcae.composition.controller.health;

import org.onap.sdc.dcae.composition.restmodels.health.ComponentsInfo;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(value = "singleton")
@Component
public class ToscaLabHealthState {
	private ComponentsInfo toscaLabHealthResponse;
	
	public ToscaLabHealthState() {
		super();
		toscaLabHealthResponse = new ComponentsInfo();
		toscaLabHealthResponse.setDescription("Not up yet");
		toscaLabHealthResponse.setHealthCheckComponent(DcaeBeConstants.Health.TOSCA_LAB);
		toscaLabHealthResponse.setHealthCheckStatus(DcaeBeConstants.Health.DOWN);
	}

	public ComponentsInfo getToscaLabHealthResponse() {
		return toscaLabHealthResponse;
	}

	public void setToscaLabHealthResponse(ComponentsInfo toscaLabHealthResponse) {
		this.toscaLabHealthResponse = toscaLabHealthResponse;
	}
	
}

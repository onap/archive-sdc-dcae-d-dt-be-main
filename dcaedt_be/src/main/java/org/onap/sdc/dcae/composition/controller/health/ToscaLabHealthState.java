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

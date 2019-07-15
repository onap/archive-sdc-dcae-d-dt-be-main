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

import java.util.ArrayList;
import java.util.List;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.health.ComponentsInfo;
import org.onap.sdc.dcae.composition.restmodels.health.HealthResponse;
import org.onap.sdc.dcae.composition.CompositionEngine;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class HealthController {
	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	Gson gson = new Gson();
	
	@Autowired
	ToscaLabHealthState toscaLabHealthState;
	
	@RequestMapping(value = "/healthCheck", method = RequestMethod.GET)
	public ResponseEntity<String> healthCheck() {
		HttpStatus httpSts = HttpStatus.OK;
		try{
			HealthResponse healthResponse = new HealthResponse();
			healthResponse.setHealthCheckComponent(DcaeBeConstants.Health.APP_NAME);
			healthResponse.setHealthCheckStatus(DcaeBeConstants.Health.UP);
			healthResponse.setSdcVersion(CompositionEngine.getDcaeVersion());
			healthResponse.setDescription(DcaeBeConstants.Health.OK);
			
			List<ComponentsInfo> componentsInfoList = new ArrayList<ComponentsInfo>();
			ComponentsInfo componentsInfo = new ComponentsInfo();
			componentsInfo.setHealthCheckComponent(DcaeBeConstants.Health.BE);
			componentsInfo.setHealthCheckStatus(DcaeBeConstants.Health.UP);
			componentsInfo.setVersion(CompositionEngine.getDcaeVersion());
			componentsInfo.setDescription(DcaeBeConstants.Health.OK);
			componentsInfoList.add(componentsInfo);
			
			ComponentsInfo toscaLab = new ComponentsInfo();
			ComponentsInfo toscaLabHealthRes = toscaLabHealthState.getToscaLabHealthResponse();
			if(toscaLabHealthRes.getHealthCheckStatus().equals(DcaeBeConstants.Health.DOWN)){
				healthResponse.setHealthCheckStatus(DcaeBeConstants.Health.DOWN);
				healthResponse.setDescription(toscaLabHealthRes.getHealthCheckComponent()+" is down");
				httpSts = HttpStatus.INTERNAL_SERVER_ERROR;
			}
			toscaLab.setHealthCheckComponent(toscaLabHealthRes.getHealthCheckComponent());
			toscaLab.setHealthCheckStatus(toscaLabHealthRes.getHealthCheckStatus());
			toscaLab.setVersion(toscaLabHealthRes.getVersion());
			toscaLab.setDescription(toscaLabHealthRes.getDescription());
			componentsInfoList.add(toscaLab);
			
			healthResponse.setComponentsInfo(componentsInfoList);
			String json = gson.toJson(healthResponse, HealthResponse.class);
			
			return new ResponseEntity<String>(json, httpSts);
		}
		catch(Exception e){
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occured while performing HealthCheck: {}", e.getLocalizedMessage());
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}

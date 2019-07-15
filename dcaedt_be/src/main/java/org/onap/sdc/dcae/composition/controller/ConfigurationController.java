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

package org.onap.sdc.dcae.composition.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.CompositionConfig;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.ves.VesStructureLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@EnableAutoConfiguration
@CrossOrigin
@RequestMapping("/conf")
public class ConfigurationController extends BaseController {

    @Autowired
    private CompositionConfig compositionConfig;

    @ApiOperation(value = "Get a list of available flow types", response = CompositionConfig.class)
    @ApiResponses(value = {
                @ApiResponse(code = 200, message = "Successfully retrieved available flow types list"),
                @ApiResponse(code = 500, message = "Flow types couldn't be fetched due to internal error")})
    @RequestMapping(value = "/composition", method = RequestMethod.GET)
    public ResponseEntity getCompositionConfig() {
        try {
            return new ResponseEntity<>(compositionConfig, HttpStatus.OK);
        }catch (Exception e) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Exception:{}",e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FLOW_TYPES_CONFIGURATION_ERROR);
        }
    }

    @RequestMapping(value = "/ves/schemaversions", method = RequestMethod.GET)
    public ResponseEntity getCommonEventFormatVersion() {
        try {
            Set<String> availableVersionsSet = VesStructureLoader.getAvailableVersionsList();
            List<String> availableVersionsList = new ArrayList<>(availableVersionsSet.size());
            availableVersionsList.addAll(availableVersionsSet);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got a request to return available ves schema versions: {}", availableVersionsSet);
            return new ResponseEntity<>(availableVersionsList, HttpStatus.OK);
        }catch (Exception e) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Exception:{}",e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.VES_SCHEMA_NOT_FOUND);
        }
    }

	@RequestMapping(value = "/getPhases/{flowType}", method = RequestMethod.GET)
	public ResponseEntity getPhasesByFlowType(@PathVariable String flowType) {
		try {
			CompositionConfig.FlowType phases = compositionConfig.getFlowTypesMap().get(flowType);
			if(null == phases) {
				phases = new CompositionConfig.FlowType();
				phases.setEntryPointPhaseName("");
				phases.setLastPhaseName("");
			}
			return new ResponseEntity<>(phases, HttpStatus.OK);
		} catch (Exception e) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Exception:{}",e);
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FLOW_TYPES_CONFIGURATION_ERROR);
		}
	}


}

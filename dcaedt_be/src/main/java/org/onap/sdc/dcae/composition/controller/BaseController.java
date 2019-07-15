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

import com.google.gson.Gson;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.composition.impl.BaseBusinessLogic;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

public abstract class BaseController {
	
	protected Gson gson = new Gson();

	@Autowired
	protected BaseBusinessLogic baseBusinessLogic;

	protected OnapLoggerError errLogger = OnapLoggerError.getInstance();
	protected OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	@ModelAttribute("requestId")
	public String getRequestId(HttpServletRequest request) {
		return request.getAttribute("requestId").toString();
	}

	ResponseEntity handleException(Exception e, ApiType apiType, String... variables){
		errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
		return ErrConfMgr.INSTANCE.handleException(e, apiType, variables);
	}
}

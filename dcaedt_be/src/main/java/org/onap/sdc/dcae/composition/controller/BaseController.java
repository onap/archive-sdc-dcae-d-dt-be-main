package org.onap.sdc.dcae.composition.controller;

import com.google.gson.Gson;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
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

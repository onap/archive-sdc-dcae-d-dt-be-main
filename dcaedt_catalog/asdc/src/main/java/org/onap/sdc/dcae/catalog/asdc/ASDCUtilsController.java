package org.onap.sdc.dcae.catalog.asdc;

import java.io.StringReader;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.springframework.beans.BeansException;

import org.springframework.web.bind.annotation.RestController;

import org.onap.sdc.dcae.catalog.asdc.ASDC;
import org.onap.sdc.dcae.catalog.asdc.ASDCUtils;
import org.onap.sdc.dcae.catalog.asdc.ASDCUtilsController;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.json.JSONObject;


@RestController
@ConfigurationProperties(prefix="asdcUtilsController")
public class ASDCUtilsController implements ApplicationContextAware {

	private ApplicationContext			appCtx;
	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	
	//Constants//
	private static String NOT_CERTIFIED_CHECKOUT = "NOT_CERTIFIED_CHECKOUT"; 
	private static String NOT_CERTIFIED_CHECKIN  = "NOT_CERTIFIED_CHECKIN"; 
	private static String CERTIFICATION_IN_PROGRESS = "CERTIFICATION_IN_PROGRESS"; 
	private static String CERTIFIED = "CERTIFIED"; 


	public void setApplicationContext(ApplicationContext theCtx) throws BeansException {
    this.appCtx = theCtx;
	}

	@PostConstruct
	public void initController() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"initASDCUtilsController");

		//Done
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"ASDCUtilsController started");
	}

	@PreDestroy
	public void cleanupController() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"cleanupASDCUtilsController");
	}

}

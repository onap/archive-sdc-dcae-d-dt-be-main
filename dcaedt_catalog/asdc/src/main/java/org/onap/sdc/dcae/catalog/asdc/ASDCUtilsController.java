package org.onap.sdc.dcae.catalog.asdc;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@RestController
@ConfigurationProperties(prefix="asdcUtilsController")
public class ASDCUtilsController implements ApplicationContextAware {

	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	public void setApplicationContext(ApplicationContext theCtx) throws BeansException {
		// no use for app context
	}

	@PostConstruct
	public void initController() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"initASDCUtilsController");
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"ASDCUtilsController started");
	}

	@PreDestroy
	public void cleanupController() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"cleanupASDCUtilsController");
	}

}

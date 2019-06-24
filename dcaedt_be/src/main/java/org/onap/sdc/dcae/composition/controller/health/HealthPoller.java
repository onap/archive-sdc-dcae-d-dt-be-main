package org.onap.sdc.dcae.composition.controller.health;

import com.google.gson.Gson;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.catalog.commons.Http;
import org.onap.sdc.dcae.composition.restmodels.health.ComponentsInfo;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.util.Collections;

@Configuration
@EnableAsync
@EnableScheduling
@ConfigurationProperties(prefix="blueprinter")
public class HealthPoller {
	private URI	hcuri;
	private String hcretrynum;
	private Gson gson;
	
	private OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	@Autowired
	private ToscaLabHealthState toscaLabHealthState;
	
	public HealthPoller() {
		super();
		gson = new Gson();
	}

	@Scheduled(fixedDelayString="${healthpoller.fixedDelay}")
	public void pollToscaLabHealth() {
		ComponentsInfo toscaLabHealthRes = null;
		ResponseEntity<String> healthRes = null;
		try {
			for (int i = 0; i < Integer.valueOf(hcretrynum); i++) { // 3 tries
				healthRes = sendHealthCheck();
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Try #{}: {}", i, healthRes);
				if (healthRes.getStatusCode() == HttpStatus.OK) {
					String result = healthRes.getBody();
					toscaLabHealthRes = gson.fromJson(result, ComponentsInfo.class);
					break;
				}
			}
		} catch (Exception e) {
			toscaLabHealthRes = getNegativeHealth(e.getMessage());
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "HealthCheck Exception: {}", e);
		}
		if (toscaLabHealthRes == null) {
			String msg = null != healthRes ? healthRes.getBody() + "-" + healthRes.getStatusCode() : "";
			toscaLabHealthRes = getNegativeHealth(msg);
		}
		toscaLabHealthState.setToscaLabHealthResponse(toscaLabHealthRes);
	}

	private ComponentsInfo getNegativeHealth(String msg) {
		ComponentsInfo toscaLabHealthRes = new ComponentsInfo();
		String description = "DCAE-D BE failed while trying to fetch Tosca_Lab healthcheck. Exception: " +msg;
		toscaLabHealthRes.setDescription(description);
		toscaLabHealthRes.setHealthCheckComponent(DcaeBeConstants.Health.TOSCA_LAB);
		toscaLabHealthRes.setHealthCheckStatus(DcaeBeConstants.Health.DOWN);
		errLogger.log(LogLevel.ERROR, this.getClass().getName(), description);
		return toscaLabHealthRes;
	}
	
	public ResponseEntity<String> sendHealthCheck() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return Http.exchangeSync(hcuri.toString(), HttpMethod.GET, entity, String.class, 5000);
	}

	public void setHcuri(URI hcuri) {
		this.hcuri = hcuri;
	}

	public void setHcretrynum(String hcretrynum) {
		this.hcretrynum = hcretrynum;
	}
	
}

package org.onap.sdc.dcae.catalog.asdc;

import java.net.URI;

import java.util.Collections;

import org.json.JSONObject;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Http;
import org.json.JSONArray;

import org.springframework.util.Base64Utils;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component("blueprinter")
@Scope("singleton")
@ConfigurationProperties(prefix="blueprinter")
public class Blueprinter {


	private URI	serviceUri;
	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


	public Blueprinter() {
	}

	public void setUri(URI theUri) {
		this.serviceUri = theUri;
	}

	public BlueprintAction generateBlueprint() {
		return new BlueprintAction();
	}

	public class BlueprintAction implements Action<String> {

		private JSONObject	body = new JSONObject();


		protected BlueprintAction() {
		}

		public BlueprintAction withModelData(byte[] theSchema, byte[] theTemplate, byte[] theTranslation) {
			return this;
		}

		public BlueprintAction withModelInfo(JSONObject theModelInfo) {
			body.append("models", theModelInfo);
			return this;
		}

		public BlueprintAction withTemplateData(byte[] theData) {
			body.put("template", Base64Utils.encodeToString(theData));
			return this;
		}

		public Future<String> execute() {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Blueprinter::execute() | PAYLOAD to TOSCA_LAB={}", body.toString());
			HttpHeaders headers = new HttpHeaders();
 			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			return Http.exchange(serviceUri.toString(), HttpMethod.POST, new HttpEntity<String>(body.toString(), headers), String.class);
		}
	}
}

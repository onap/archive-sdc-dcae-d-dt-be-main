package org.onap.sdc.dcae.catalog.asdc;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.JSONHttpMessageConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component("asdc")
@Scope("singleton")
public class ASDC {

	public enum AssetType {
		resource,
		service,
		product
	}

	protected static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	protected static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	private URI rootUri;
	private String rootPath = "/sdc/v1/catalog/";
	private String user, passwd;
	private String instanceId;

	public void setUri(URI theUri) {
		String userInfo = theUri.getUserInfo();
		if (userInfo != null) {
			String[] userInfoParts = userInfo.split(":");
			setUser(userInfoParts[0]);
			if (userInfoParts.length > 1) {
				setPassword(userInfoParts[1]);
			}
		}
		String fragment = theUri.getFragment();
		if (fragment == null) {
			throw new IllegalArgumentException("The URI must contain a fragment specification, to be used as ASDC instance id");
		}
		setInstanceId(fragment);

		try {
			this.rootUri = new URI(theUri.getScheme(), null, theUri.getHost(), theUri.getPort(), theUri.getPath(), theUri.getQuery(), null);
		}
		catch (URISyntaxException urix) {
			throw new IllegalArgumentException("Invalid uri", urix);
		}
 	}

	public URI getUri() {
		return this.rootUri;	
	}

	public void setUser(String theUser) {
		this.user = theUser;
	}

	public String getUser() {
		return this.user;
	}

	public void setPassword(String thePassword) {
		this.passwd = thePassword;
	}

	public String getPassword() {
		return this.passwd;
	}

	public void setInstanceId(String theId) {
		this.instanceId = theId;
	}

    @Scheduled(fixedRateString = "${beans.context.scripts.updateCheckFrequency?:60000}")
	public void checkForUpdates() {
		// ffu
	}

	@PostConstruct
	public void initASDC() {
		// ffu
	}

	public <T> Future<T> getResources(Class<T> theType) {
		return getAssets(AssetType.resource, theType);
	}
	
	public Future<JSONArray> getResources() {
		return getAssets(AssetType.resource, JSONArray.class);
	}
	
	public <T> Future<T> getResources(Class<T> theType, String theCategory, String theSubCategory) {
		return getAssets(AssetType.resource, theType, theCategory, theSubCategory);
	}
	
	public Future<JSONArray> getResources(String category, String subCategory, String resourceType) {
		return getAssets(AssetType.resource, JSONArray.class, category, subCategory, resourceType);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType) {
		return fetch(refAssets(theAssetType), theType);
	}
	
	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory) {
		return getAssets(theAssetType, theType, theCategory, theSubCategory, null);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory, String theResourceType) {
		return fetch(refAssets(theAssetType) + filter(theCategory, theSubCategory, theResourceType), theType);
	}

	protected String refAssets(AssetType theAssetType) {
		return this.rootPath + theAssetType + "s/";
	}

	private String filter(String theCategory, String theSubCategory, String theResourceType) {
		StringBuilder filter = null;
		if (theCategory != null) {
			filter = new StringBuilder();
			filter.append("?category=")
						.append(theCategory);
			if (theSubCategory != null) {
				filter.append("&subCategory=")
							.append(theSubCategory);
				if (theResourceType != null) {
					filter.append("&resourceType=")
								.append(theResourceType);
				}
			}
		}
		return filter == null ? "" : filter.toString();
	}

	protected String refAsset(AssetType theAssetType, UUID theId) {
		return this.rootPath + theAssetType + "s/" + theId;
	}
	
	public <T> Future<T> getResource(UUID theId, Class<T> theType) {
		return getAsset(AssetType.resource, theId, theType);
	}
	
	public Future<JSONObject> getResource(UUID theId) {
		return getAsset(AssetType.resource, theId, JSONObject.class);
	}

	public <T> Future<T> getAsset(AssetType theAssetType, UUID theId, Class<T> theType) {
		return fetch(refAsset(theAssetType, theId) + "/metadata", theType);
	}
	
	public <T> Action<T> getAssetAction(AssetType theAssetType, UUID theId, Class<T> theType) {
		return () -> fetch(refAsset(theAssetType, theId) + "/metadata", theType);
	}

	public Future<byte[]> getAssetArchive(AssetType theAssetType, UUID theId) {
		return fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class);
	}

	public Action<byte[]> getAssetArchiveAction(AssetType theAssetType, UUID theId) {
		return () -> fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class);
	}

	protected String refAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId) {
		return refAsset(theAssetType, theAssetId) + "/artifacts" + (theArtifactId == null ? "" : ("/" + theArtifactId));
	}
	
	public <T> Future<T> getResourceArtifact(UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return getAssetArtifact(AssetType.resource, theAssetId, theArtifactId, theType);
	}

	public <T> Future<T> getAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return fetch(refAssetArtifact(theAssetType, theAssetId, theArtifactId), theType);
	}


	public static JSONObject merge(JSONObject theOriginal, JSONObject thePatch) {
		for (String key: (Set<String>)thePatch.keySet()) {
			if (!theOriginal.has(key)) {
				theOriginal.put(key, thePatch.get(key));
			}
		}
		return theOriginal;
	}

	private HttpHeaders prepareHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString((this.user + ":" + this.passwd).getBytes()));
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
		headers.add("X-ECOMP-InstanceID", this.instanceId);

		return headers;
	}

	public <T> Future<T> fetch(String theRef, Class<T> theContentType) {
		return exchange(theRef, HttpMethod.GET, new HttpEntity(prepareHeaders()), theContentType);
	}

	public <T> Future<T> exchange(String theRef, HttpMethod theMethod, HttpEntity theRequest, Class<T> theResponseType) {
		
		AsyncRestTemplate restTemplate = new AsyncRestTemplate();

		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0, new JSONHttpMessageConverter());
		restTemplate.setMessageConverters(converters);

		restTemplate.setInterceptors(Collections.singletonList(new ContentMD5Interceptor()));
		ASDCFuture<T> result = new ASDCFuture<T>();
		String uri = this.rootUri + theRef;
		try {
			restTemplate
				.exchange(uri, theMethod, theRequest, theResponseType)
					.addCallback(result.callback);
		}
		catch (RestClientException rcx) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(), "Failed to fetch {} {}", uri, rcx);
			return Futures.failedFuture(rcx);
		}
		catch (Exception x) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(), "Failed to fetch {} {}", uri, x);
			return Futures.failedFuture(x);
		}
	 
		return result;
	}

	public class ASDCFuture<T> extends Futures.BasicFuture<T> {

		ListenableFutureCallback<ResponseEntity<T>> callback = new ListenableFutureCallback<ResponseEntity<T>>() {

			public void	onSuccess(ResponseEntity<T> theResult) {
				ASDCFuture.this.result(theResult.getBody());
			}

			public void	onFailure(Throwable theError) {
				if (theError instanceof HttpClientErrorException) {
						ASDCFuture.this.cause(new ASDCException((HttpClientErrorException)theError));
				}
				else {
					ASDCFuture.this.cause(theError);
				}
			}
		};
	}

	public class ContentMD5Interceptor implements AsyncClientHttpRequestInterceptor {
		@Override
		public ListenableFuture<ClientHttpResponse> intercept(
				HttpRequest theRequest, byte[] theBody, AsyncClientHttpRequestExecution theExecution)
				throws IOException {
			if (HttpMethod.POST == theRequest.getMethod()) {
				HttpHeaders headers = theRequest.getHeaders();
				headers.add("Content-MD5", Base64Utils.encodeToString(DigestUtils.md5Hex(theBody).getBytes()));
			}
			return theExecution.executeAsync(theRequest, theBody);
		}
	}
}

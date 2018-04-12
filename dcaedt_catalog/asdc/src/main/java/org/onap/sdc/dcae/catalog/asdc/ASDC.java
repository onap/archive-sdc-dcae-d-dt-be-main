package org.onap.sdc.dcae.catalog.asdc;

import java.net.URI;
import java.net.URISyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;

import java.util.function.UnaryOperator;

import javax.annotation.PostConstruct;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.enums.ArtifactGroupType;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.converter.HttpMessageConverter;

import org.springframework.util.Base64Utils;
//import org.springframework.util.DigestUtils;
import org.apache.commons.codec.digest.DigestUtils;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.json.JSONObject;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.JSONHttpMessageConverter;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.json.JSONArray;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


@Component("asdc")
@Scope("singleton")
//@ConfigurationProperties(prefix="asdc")
public class ASDC {

	public static enum AssetType {
		resource,
		service,
		product
	}

//	public static enum ArtifactType {
//		DCAE_TOSCA,
//		DCAE_JSON,
//		DCAE_POLICY,
//		DCAE_DOC,
//		DCAE_EVENT,
//		DCAE_INVENTORY_TOSCA,
//		DCAE_INVENTORY_JSON,
//		DCAE_INVENTORY_POLICY,
//		DCAE_INVENTORY_DOC,
//		DCAE_INVENTORY_BLUEPRINT,
//		DCAE_INVENTORY_EVENT,
//		HEAT,
//		HEAT_VOL,
//		HEAT_NET,
//		HEAT_NESTED,
//		HEAT_ARTIFACT,
//		HEAT_ENV,
//		OTHER
//	}

//	public static enum ArtifactGroupType {
//		DEPLOYMENT,
//		INFORMATIONAL
//	}

	public static enum LifecycleState {
		Checkin,
		Checkout,
		Certify,
		undocheckout
	}

	
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.METHOD)
//	public @interface Mandatory {
//	}

	protected static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	protected static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	
	@Autowired
	private SystemProperties systemProperties;

	private URI			rootUri;
	private String	rootPath = "/sdc/v1/catalog/";
	private String	user,
									passwd;
	private String	instanceId;


	public void setUri(URI theUri) {
		//theUri = URI.create(systemProperties.getProperties().getProperty(SystemProperties.ASDC_CATALOG_URL));
		String userInfo = theUri.getUserInfo();
		if (userInfo != null) {
			String[] userInfoParts = userInfo.split(":");
			setUser(userInfoParts[0]);
			if (userInfoParts.length > 1)
				setPassword(userInfoParts[1]);
		}
		String fragment = theUri.getFragment();
		if (fragment == null)
			throw new IllegalArgumentException("The URI must contain a fragment specification, to be used as ASDC instance id");
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

	public String getInstanceId() {
		return this.instanceId;
	}
	
	public void setRootPath(String thePath) {
		this.rootPath = systemProperties.getProperties().getProperty(DcaeBeConstants.Config.ASDC_ROOTPATH);
	}

	public String getRootPath() {
		return systemProperties.getProperties().getProperty(DcaeBeConstants.Config.ASDC_ROOTPATH);
	}

    @Scheduled(fixedRateString = "${beans.context.scripts.updateCheckFrequency?:60000}")
	public void checkForUpdates() { 
	}

	@PostConstruct
	public void initASDC() {
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

	public <T> Future<T> getServices(Class<T> theType) {
		return getAssets(AssetType.service, theType);
	}
	
	public Future<JSONArray> getServices() {
		return getAssets(AssetType.service, JSONArray.class);
	}
	
	public <T> Future<T> getServices(Class<T> theType, String theCategory, String theSubCategory) {
		return getAssets(AssetType.service, theType, theCategory, theSubCategory);
	}
	
	public Future<JSONArray> getServices(String theCategory, String theSubCategory) {
		return getAssets(AssetType.service, JSONArray.class, theCategory, theSubCategory);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType) {
		return fetch(refAssets(theAssetType), theType);
	}
	
	public <T> Action<T> getAssetsAction(AssetType theAssetType, Class<T> theType) {
		return (() -> fetch(refAssets(theAssetType), theType));
	}
	
	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory) {
		return getAssets(theAssetType, theType, theCategory, theSubCategory, null);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory, String theResourceType) {
		return fetch(refAssets(theAssetType) + filter(theCategory, theSubCategory, theResourceType), theType);
	}
	
	public <T> Action<T> getAssetsAction(AssetType theAssetType, Class<T> theType,
																 			 String theCategory, String theSubCategory, String theResourceType) {
		return (() -> fetch(refAssets(theAssetType) + filter(theCategory, theSubCategory, theResourceType), theType));
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

	public Future<ResourceDetailed> getSDCResource(UUID theId) {
		return getAsset(AssetType.resource, theId, ResourceDetailed.class);
	}


	public <T> Future<T> getService(UUID theId, Class<T> theType) {
		return getAsset(AssetType.service, theId, theType);
	}
	
	public Future<JSONObject> getService(UUID theId) {
		return getAsset(AssetType.service, theId, JSONObject.class);
	}

	public <T> Future<T> getAsset(AssetType theAssetType, UUID theId, Class<T> theType) {
		return fetch(refAsset(theAssetType, theId) + "/metadata", theType);
	}
	
	public <T> Action<T> getAssetAction(AssetType theAssetType, UUID theId, Class<T> theType) {
		return (() -> fetch(refAsset(theAssetType, theId) + "/metadata", theType));
	}

	public Future<byte[]> getResourceArchive(UUID theId) {
		return getAssetArchive(AssetType.resource, theId);
	}

	public Future<byte[]> getServiceArchive(UUID theId) {
		return getAssetArchive(AssetType.service, theId);
	}

	public Future<byte[]> getAssetArchive(AssetType theAssetType, UUID theId) {
		return fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class);
	}

	public Action<byte[]> getAssetArchiveAction(AssetType theAssetType, UUID theId) {
		return (() -> fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class));
	}

	public Future<JSONObject> checkinResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Checkin, theUser, theMessage);
	}

	public Future<JSONObject> checkinService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Checkin, theUser, theMessage);
	}

	public Future<JSONObject> checkoutResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Checkout, theUser, theMessage);
	}

	public Future<JSONObject> checkoutService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Checkout, theUser, theMessage);
	}
	
	public Future<JSONObject> certifyResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Certify, theUser, theMessage);
	}

	public Future<JSONObject> certifyService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Certify, theUser, theMessage);
	}

	/* Normally theMessage is mandatory (and we'd use put instead of putOpt) but .. not so for undocheckout ..
	 */
	public Future<JSONObject> cycleAsset(AssetType theAssetType, UUID theId, LifecycleState theState,
																			 String theUser, String theMessage) {
		return post(refAsset(theAssetType, theId)	+ "/lifecycleState/" + theState,
							  (headers) -> prepareHeaders(headers)
															.header("USER_ID", theUser),
								new JSONObject().putOpt("userRemarks", theMessage));
	}

	protected String refAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theAssetInstance, UUID theArtifactId) {
		return refAsset(theAssetType, theAssetId) + "/resourceInstances/" + theAssetInstance + "/artifacts" + (theArtifactId == null ? "" : ("/" + theArtifactId));
	}

	protected String refAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId) {
		return refAsset(theAssetType, theAssetId) + "/artifacts" + (theArtifactId == null ? "" : ("/" + theArtifactId));
	}
	
	public <T> Future<T> getResourceArtifact(UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return getAssetArtifact(AssetType.resource, theAssetId, theArtifactId, theType);
	}
	
	public <T> Future<T> getServiceArtifact(UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return getAssetArtifact(AssetType.service, theAssetId, theArtifactId, theType);
	}
	
	public <T> Future<T> getResourceInstanceArtifact(UUID theAssetId, UUID theArtifactId, String theInstance, Class<T> theType) {
		return getAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactId, theType);
	}
	
	public <T> Future<T> getServiceInstanceArtifact(UUID theAssetId, UUID theArtifactId, String theInstance, Class<T> theType) {
		return getAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactId, theType);
	}

	public <T> Future<T> getAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return fetch(refAssetArtifact(theAssetType, theAssetId, theArtifactId), theType);
	}
	
	public <T> Action<T> getAssetArtifactAction(AssetType theAssetType, UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return (() -> fetch(refAssetArtifact(theAssetType, theAssetId, theArtifactId), theType));
	}
	
	public <T> Future<T> getAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId, Class<T> theType) {
		return fetch(refAssetInstanceArtifact(theAssetType, theAssetId, theInstance, theArtifactId), theType);
	}
	
	public <T> Action<T> getAssetInstanceArtifactAction(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId, Class<T> theType) {
		return (() -> fetch(refAssetInstanceArtifact(theAssetType, theAssetId, theInstance, theArtifactId), theType));
	}
	
	public ArtifactUploadAction createResourceArtifact(UUID theAssetId) {
		return createAssetArtifact(AssetType.resource, theAssetId);
	}
	
	public ArtifactUploadAction createServiceArtifact(UUID theAssetId) {
		return createAssetArtifact(AssetType.service, theAssetId);
	}
	
	public ArtifactUploadAction createResourceInstanceArtifact(UUID theAssetId, String theInstance) {
		return createAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance);
	}
	
	public ArtifactUploadAction createServiceInstanceArtifact(UUID theAssetId, String theInstance) {
		return createAssetInstanceArtifact(AssetType.service, theAssetId, theInstance);
	}

	public ArtifactUploadAction createAssetArtifact(AssetType theAssetType, UUID theAssetId) {
		return new ArtifactUploadAction()
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactUploadAction createAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance) {
		return new ArtifactUploadAction()
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	public ArtifactUpdateAction updateResourceArtifact(UUID theAssetId, JSONObject theArtifactInfo) {
		return updateAssetArtifact(AssetType.resource, theAssetId, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateResourceInstanceArtifact(UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return updateAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateServiceArtifact(UUID theAssetId, JSONObject theArtifactInfo) {
		return updateAssetArtifact(AssetType.service, theAssetId, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateServiceInstanceArtifact(UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return updateAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactInfo);
	}

	public ArtifactUpdateAction updateAssetArtifact(AssetType theAssetType, UUID theAssetId, JSONObject theArtifactInfo) {
		return new ArtifactUpdateAction(theArtifactInfo)
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactUpdateAction updateAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return new ArtifactUpdateAction(theArtifactInfo)
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	public ArtifactDeleteAction deleteResourceArtifact(UUID theAssetId, UUID theArtifactId) {
		return deleteAssetArtifact(AssetType.resource, theAssetId, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteResourceInstanceArtifact(UUID theAssetId, String theInstance, UUID theArtifactId) {
		return deleteAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteServiceArtifact(UUID theAssetId, UUID theArtifactId) {
		return deleteAssetArtifact(AssetType.service, theAssetId, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteServiceInstanceArtifact(UUID theAssetId, String theInstance, UUID theArtifactId) {
		return deleteAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactId);
	}

	public ArtifactDeleteAction deleteAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId) {
		return new ArtifactDeleteAction(theArtifactId)
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactDeleteAction deleteAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId) {
		return new ArtifactDeleteAction(theArtifactId)
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	
	public abstract class ASDCAction<A extends ASDCAction<A, T>, T> implements Action<T> { 

		protected JSONObject 	info; 				//info passed to asdc as request body
		protected String			operatorId;		//id of the SDC user performing the action

		protected ASDCAction(JSONObject theInfo) {
			this.info = theInfo;
		}
		
		protected abstract A self(); 

		protected ASDC asdc() {
			return ASDC.this;
		}
	
		protected A withInfo(JSONObject theInfo) {
			merge(this.info, theInfo);
			return self();
		}
	
		public A with(String theProperty, Object theValue) {
			info.put(theProperty, theValue);
			return self();
		}

		public A withOperator(String theOperator) {
			this.operatorId = theOperator;
			return self();			
		}
		
		protected abstract String[] mandatoryInfoEntries();
	
		protected void checkOperatorId() {
			if (this.operatorId == null) {
				throw new IllegalStateException("No operator id was provided");
			}
		}

		protected void checkMandatoryInfo() {
			for (String field: mandatoryInfoEntries()) {
				if (!info.has(field))			
					throw new IllegalStateException("No '" + field + "' was provided");
			}
		}
		
		protected void checkMandatory() {
			checkOperatorId();
			checkMandatoryInfo();
		}
	}

	protected static final String[] artifactMandatoryEntries = new String[] {};

	/**
   * We use teh same API to operate on artifacts attached to assets or to their instances
	 */
	public abstract class ASDCArtifactAction<A extends ASDCArtifactAction<A>> extends ASDCAction<A, JSONObject> {

		protected AssetType		assetType;
		protected UUID				assetId;
		protected String			assetInstance;

		protected ASDCArtifactAction(JSONObject theInfo) {
			super(theInfo);
		}
		
		protected A ofAsset(AssetType theAssetType, UUID theAssetId) {
			this.assetType = theAssetType;
			this.assetId = theAssetId;
			return self();			
		}
		
		protected A ofAssetInstance(AssetType theAssetType, UUID theAssetId, String theInstance) {
			this.assetType = theAssetType;
			this.assetId = theAssetId;
			this.assetInstance = theInstance;
			return self();			
		}
		
		protected String normalizeInstanceName(String theName) {
			return StringUtils.removePattern(theName, "[ \\.\\-]+").toLowerCase();
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.artifactMandatoryEntries;
		}

		protected String ref(UUID theArtifactId) {
			return (this.assetInstance == null) ?
								refAssetArtifact(this.assetType, this.assetId, theArtifactId) :
								refAssetInstanceArtifact(this.assetType, this.assetId, normalizeInstanceName(this.assetInstance), theArtifactId);
		}
	} 

	protected static final String[] uploadMandatoryEntries = new String[] { "artifactName",
																																					 "artifactType",
																																					 "artifactGroupType", 
																																					 "artifactLabel",
																																					 "description",
																																					 "payloadData" };

	public class ArtifactUploadAction extends ASDCArtifactAction<ArtifactUploadAction> {
		
		protected ArtifactUploadAction() {
			super(new JSONObject());
		}

		protected ArtifactUploadAction self() {
			return this;
		}
		
		public ArtifactUploadAction withContent(byte[] theContent) {
			return with("payloadData", Base64Utils.encodeToString(theContent));
		}

		public ArtifactUploadAction withContent(File theFile) throws IOException {
			return withContent(FileUtils.readFileToByteArray(theFile));
		}

		public ArtifactUploadAction withLabel(String theLabel) {
			return with("artifactLabel", theLabel);
		}
		
		public ArtifactUploadAction withName(String theName) {
			return with("artifactName", theName);
		}
		
		public ArtifactUploadAction withDisplayName(String theName) {
			return with("artifactDisplayName", theName);
		}

		public ArtifactUploadAction withType(ArtifactType theType) {
			return with("artifactType", theType.toString());
		}

		public ArtifactUploadAction withGroupType(ArtifactGroupType theGroupType) {
			return with("artifactGroupType", theGroupType.toString());
		}

		public ArtifactUploadAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.uploadMandatoryEntries;
		}

		public Future<JSONObject> execute() {
			checkMandatory();
			return ASDC.this.post(ref(null),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}
	}

	protected static final String[] updateMandatoryEntries = new String[] { "artifactName",
																																					 "artifactType",
																																					 "artifactGroupType", 
																																					 "artifactLabel",
																																					 "description",
																																					 "payloadData" };

	/**
	 * In its current form the update relies on a previous artifact retrieval. One cannot build an update from scratch.
	 * The label, tye and group type must be submitted but cannot be updated
	 */
	public class ArtifactUpdateAction extends ASDCArtifactAction<ArtifactUpdateAction> {

		
		protected ArtifactUpdateAction(JSONObject theInfo) {
			super(theInfo);
		}
		
		protected ArtifactUpdateAction self() {
			return this;
		}
		
		public ArtifactUpdateAction withContent(byte[] theContent) {
			return with("payloadData", Base64Utils.encodeToString(theContent));
		}

		public ArtifactUpdateAction withContent(File theFile) throws IOException {
			return withContent(FileUtils.readFileToByteArray(theFile));
		}

		public ArtifactUpdateAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		public ArtifactUpdateAction withName(String theName) {
			return with("artifactName", theName);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.updateMandatoryEntries;
		}

		/* The json object originates (normally) from a get so it will have entries we need to cleanup */
		protected void cleanupInfoEntries() {
			this.info.remove("artifactChecksum");
			this.info.remove("artifactUUID");
			this.info.remove("artifactVersion");
			this.info.remove("artifactURL");
			this.info.remove("artifactDescription");
		}
		
		public Future<JSONObject> execute() {
			UUID artifactUUID = UUID.fromString(this.info.getString("artifactUUID"));
			checkMandatory();
			cleanupInfoEntries();
			return ASDC.this.post(ref(artifactUUID),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}
	}

	public class ArtifactDeleteAction extends ASDCArtifactAction<ArtifactDeleteAction> {

		private UUID		artifactId;
		
		protected ArtifactDeleteAction(UUID theArtifactId) {
			super(null);
			this.artifactId = theArtifactId;
		}
		
		protected ArtifactDeleteAction self() {
			return this;
		}
		
		public Future<JSONObject> execute() {
			checkMandatory();
			return ASDC.this.delete(ref(this.artifactId),
														  (headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId));
		}
	}




	public VFCMTCreateAction createVFCMT() {
		return new VFCMTCreateAction();
	}
	
	protected static final String[] vfcmtMandatoryEntries = new String[] { "name",
																																				 "vendorName",
																																	 			 "vendorRelease",
																																				 "contactId" };


	public class VFCMTCreateAction extends ASDCAction<VFCMTCreateAction, JSONObject> {

		protected VFCMTCreateAction() {

			super(new JSONObject());
			this
				.with("resourceType", "VFCMT")
				.with("category", "Template")
				.with("subcategory", "Monitoring Template")
				.with("icon", "defaulticon");
		}
		
		protected VFCMTCreateAction self() {
			return this;
		}

		public VFCMTCreateAction withName(String theName) {
			return with("name", theName);
		}

		public VFCMTCreateAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		public VFCMTCreateAction withVendorName(String theVendorName) {
			return with("vendorName", theVendorName);
		}
		
		public VFCMTCreateAction withVendorRelease(String theVendorRelease) {
			return with("vendorRelease", theVendorRelease);
		}
		
		public VFCMTCreateAction withTags(String... theTags) {
			for (String tag: theTags)
				this.info.append("tags", tag);
			return this;			
		}
		
		public VFCMTCreateAction withIcon(String theIcon) {
			return with("icon", theIcon);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.vfcmtMandatoryEntries;
		}
		
		public VFCMTCreateAction withContact(String theContact) {
			return with("contactId", theContact);
		}
		
		public Future<JSONObject> execute() {
		
			this.info.putOnce("contactId", this.operatorId);	
			this.info.append("tags", info.optString("name"));
			checkMandatory();
			return ASDC.this.post(refAssets(AssetType.resource),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}

	}

	public static JSONObject merge(JSONObject theOriginal, JSONObject thePatch) {
		for (String key: (Set<String>)thePatch.keySet()) {
			if (!theOriginal.has(key))
				theOriginal.put(key, thePatch.get(key));
		}
		return theOriginal;
	}

	protected URI refUri(String theRef) {
		try {
			return new URI(this.rootUri + theRef);
		}
		catch(URISyntaxException urisx) {
			throw new UncheckedIOException(new IOException(urisx));
		}
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

	private RequestEntity.HeadersBuilder prepareHeaders(RequestEntity.HeadersBuilder theBuilder) {
		return theBuilder
			.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString((this.user + ":" + this.passwd).getBytes()))
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
			.header("X-ECOMP-InstanceID", this.instanceId);
	}

	public <T> Future<T> fetch(String theRef, Class<T> theContentType) {
		return exchange(theRef, HttpMethod.GET, new HttpEntity(prepareHeaders()), theContentType);
	}

	public Future<JSONObject> post(String theRef, JSONObject thePost) {
		return exchange(theRef, HttpMethod.POST, new HttpEntity<JSONObject>(thePost, prepareHeaders()), JSONObject.class);
	}
	
	public Future<JSONObject> post(String theRef, UnaryOperator<RequestEntity.HeadersBuilder> theHeadersBuilder, JSONObject thePost) {
		RequestEntity.BodyBuilder builder = RequestEntity.post(refUri(theRef));
		theHeadersBuilder.apply(builder);

		return exchange(theRef, HttpMethod.POST, builder.body(thePost), JSONObject.class);
	}
	
	public Future<JSONObject> delete(String theRef, UnaryOperator<RequestEntity.HeadersBuilder> theHeadersBuilder) {

		RequestEntity.HeadersBuilder builder = RequestEntity.delete(refUri(theRef));
		theHeadersBuilder.apply(builder);

		return exchange(theRef, HttpMethod.DELETE, builder.build(), JSONObject.class);
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



	public class ASDCFuture<T>
										extends Futures.BasicFuture<T> {

		private boolean http404toEmpty = false;

		ASDCFuture() {
		}

		public ASDCFuture setHttp404ToEmpty(boolean doEmpty) {
			this.http404toEmpty = doEmpty;
			return this;
		}

		ListenableFutureCallback<ResponseEntity<T>> callback = new ListenableFutureCallback<ResponseEntity<T>>() {

			public void	onSuccess(ResponseEntity<T> theResult) {
				ASDCFuture.this.result(theResult.getBody());
			}

			public void	onFailure(Throwable theError) {
				if (theError instanceof HttpClientErrorException) {
				//	if (theError.getRawStatusCode() == 404 && this.http404toEmpty)
				//		ASDCFuture.this.result(); //th eresult is of type T ...
				//	else
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
  	      headers.add("Content-MD5", Base64Utils.encodeToString(
																				//DigestUtils.md5Digest(theBody)));
																				DigestUtils.md5Hex(theBody).getBytes()));
																					
				}
    	  return theExecution.executeAsync(theRequest, theBody);
    }
	}

	public static void main(String[] theArgs) throws Exception {

		CommandLineParser parser = new BasicParser();
		
		String user_id = "jh0003";
		
		Options options = new Options();
		options.addOption(OptionBuilder
														  .withArgName("target")
															.withLongOpt("target")
                               .withDescription("target asdc system")
															.hasArg()
															.isRequired()
															.create('t') );
			
		options.addOption(OptionBuilder
														  .withArgName("action")
															.withLongOpt("action")
                              .withDescription("one of: list, get, getartifact, checkin, checkout")
															.hasArg()
															.isRequired()
															.create('a') );

		options.addOption(OptionBuilder
														  .withArgName("assetType")
															.withLongOpt("assetType")
                               .withDescription("one of resource, service, product")
															.hasArg()
															.isRequired()
															.create('k') ); //k for 'kind' ..

		options.addOption(OptionBuilder
														  .withArgName("assetId")
															.withLongOpt("assetId")
                               .withDescription("asset uuid")
															.hasArg()
															.create('u') ); //u for 'uuid'

		options.addOption(OptionBuilder
														  .withArgName("artifactId")
															.withLongOpt("artifactId")
                               .withDescription("artifact uuid")
															.hasArg()
															.create('s') ); //s for 'stuff'

		options.addOption(OptionBuilder
														  .withArgName("listFilter")
															.withLongOpt("listFilter")
                               .withDescription("filter for list operations")
															.hasArg()
															.create('f') ); //u for 'uuid'

		CommandLine line = null;
		try {
   		line = parser.parse(options, theArgs);
		}
		catch(ParseException exp) {
			errLogger.log(LogLevel.ERROR, ASDC.class.getName(), exp.getMessage());
			new HelpFormatter().printHelp("asdc", options);
			return;
		}

		ASDC asdc = new ASDC();
		asdc.setUri(new URI(line.getOptionValue("target")));

		String action = line.getOptionValue("action");
		if (action.equals("list")) {
			JSONObject filterInfo = new JSONObject(
																			line.hasOption("listFilter") ?
																				line.getOptionValue("listFilter") : "{}");
			JSONArray assets = 
				asdc.getAssets(ASDC.AssetType.valueOf(line.getOptionValue("assetType")), JSONArray.class,
											 filterInfo.optString("category", null), filterInfo.optString("subCategory", null))
						.waitForResult();
			for (int i = 0; i < assets.length(); i++) {
				debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),"> {}", assets.getJSONObject(i).toString(2));
			}
		}
		else if (action.equals("get")) {
			debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),
					asdc.getAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
											UUID.fromString(line.getOptionValue("assetId")),
											JSONObject.class)
						.waitForResult()
						.toString(2)
			);
		}
		else if (action.equals("getartifact")) {
			debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),
					asdc.getAssetArtifact(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
															UUID.fromString(line.getOptionValue("assetId")),
															UUID.fromString(line.getOptionValue("artifactId")),
															String.class)
						.waitForResult()
			);
		}
		else if (action.equals("checkin")) {
			debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),
					asdc.cycleAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
													UUID.fromString(line.getOptionValue("assetId")),
													ASDC.LifecycleState.Checkin,
													user_id,
													"cli op")
							.waitForResult()
							.toString()
			);
		}
		else if (action.equals("checkout")) {
			debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),
					asdc.cycleAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
													UUID.fromString(line.getOptionValue("assetId")),
													ASDC.LifecycleState.Checkout,
													user_id,
													"cli op")
							.waitForResult()
							.toString()
			);
		}
		else if (action.equals("cleanup")) {
			JSONArray resources = asdc.getResources()
																	.waitForResult();
			debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),"Got {} resources", resources.length());

			// vfcmt cleanup 
			for (int i = 0; i < resources.length(); i++) {

				JSONObject resource = resources.getJSONObject(i);

				if (resource.getString("resourceType").equals("VFCMT") &&
						resource.getString("name").contains("test")) {

					debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),"undocheckout for {}", resource.getString("uuid"));

					try {
						asdc.cycleAsset(AssetType.resource, UUID.fromString(resource.getString("uuid")), LifecycleState.undocheckout, user_id, null)
							.waitForResult();
					}
					catch (Exception x) {
						debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),"** {}", x);
					}
				}
			}

		}
		else {
			try {
				debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),
					asdc.createVFCMT()
							.withName("Clonator")
							.withDescription("Clone operation target 06192017")
							.withVendorName("CloneInc")
							.withVendorRelease("1.0")
							.withTags("clone")
							.withOperator(user_id)
							.execute()
							.waitForResult()
							.toString()
				);
			}
			catch(Exception x) {
				debugLogger.log(LogLevel.DEBUG, ASDC.class.getName(),"Failed to create VFCMT: {}", x);
			}
		}
	}
}

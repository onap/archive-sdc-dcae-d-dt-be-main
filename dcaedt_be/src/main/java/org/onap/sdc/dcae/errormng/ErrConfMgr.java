package org.onap.sdc.dcae.errormng;

import org.onap.sdc.dcae.catalog.asdc.ASDCException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

public enum ErrConfMgr {
	INSTANCE;

	private static EnumMap<ApiType, Map<String, String>> sdcDcaeMsgIdMap;
	public static final String AS_IS = "AS_IS";
	private ResponseFormatManager responseFormatManager;

	ErrConfMgr() {
		responseFormatManager = ResponseFormatManager.getInstance();
		populateSdcDcaeMsgIdMap();
	}

	private void setSdcCatalogPolicyMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("POL5000", AS_IS);
		map.put("POL5001", "POL5500");
		map.put("POL5002", "POL5501");
		sdcDcaeMsgIdMap.put(ApiType.ALL_SDC_CATALOG, map);
	}

	private void setGetVfcmtMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		map.put("SVC4505", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.GET_VFCMT, map);
	}

	private void setCreateNewVfcmtMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4050", AS_IS);
		map.put("SVC4126", AS_IS);
		map.put("SVC4500", AS_IS);
		map.put("SVC4062", AS_IS);
		map.put("SVC4064", AS_IS);
		map.put("SVC4065", AS_IS);
		map.put("SVC4066", AS_IS);
		map.put("SVC4067", AS_IS);
		map.put("SVC4068", AS_IS);
		map.put("SVC4069", AS_IS);
		map.put("SVC4070", AS_IS);
		map.put("SVC4071", AS_IS);
		map.put("SVC4072", AS_IS);
		map.put("SVC4073", AS_IS);
		map.put("SVC4053", AS_IS);
		map.put("POL5003", AS_IS);
		// adding service referencing error handling to create scenario
		map.put("SVC4063", AS_IS);
		map.put("SVC4122", AS_IS);
		map.put("SVC4124", AS_IS);
		map.put("SVC4128", AS_IS);
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", AS_IS);
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.CREATE_NEW_VFCMT, map);
	}

	private void setCloneVfcmtMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		map.put("SVC4505", AS_IS);
		map.put("SVC4085", AS_IS);
		map.put("SVC4080", AS_IS);
		map.put("SVC4122", "SVC6010");
		map.put("SVC4124", "SVC6010");
		map.put("SVC4128", "SVC6010");
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", "SVC6010");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		map.put("SVC4086", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.CLONE_VFCMT, map);
	}


	private void setGetServiceMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4503", AS_IS);
		map.put("SVC4642", "200");
		sdcDcaeMsgIdMap.put(ApiType.GET_SERVICE, map);
	}

	private void setAttachToServiceMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", "SVC6021");
		map.put("SVC4122", "SVC6021");
		map.put("SVC4124", "SVC6021");
		map.put("SVC4128", "SVC6021");
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", "SVC6021");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		map.put("SVC4503", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.ATTACH_TO_SERVICE, map);
	}

	private void setGetCdumpMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		map.put("SVC4505", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.GET_CDUMP, map);
	}

	private void setGetModelMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		map.put("SVC4505", "SVC6031");
		sdcDcaeMsgIdMap.put(ApiType.GET_MODEL, map);
	}

	private void setCheckoutResourceMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", "SVC6021");
		map.put("SVC4085", AS_IS);
		map.put("SVC4080", AS_IS);
		map.put("SVC4002", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.CHECK_OUT_RESOURCE, map);
	}

	private void setCheckinResourceMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", "SVC6021");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		map.put("SVC4084", AS_IS);
		map.put("SVC4085", AS_IS);
		map.put("SVC4002", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.CHECK_IN_RESOURCE, map);
	}

	private void setSaveCdumpMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", "SVC6021");
		map.put("SVC4122", "SVC6021");
		map.put("SVC4124", "SVC6021");
		map.put("SVC4128", "SVC6021");
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", "SVC6021");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.SAVE_CDUMP, map);
	}

	private void setSubmitBlueprintMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		map.put("SVC4505", "SVC6031");
		map.put("SVC4503", AS_IS);
		map.put("SVC4085", AS_IS);
		map.put("SVC4080", AS_IS);
		map.put("SVC4122", "SVC6033");
		map.put("SVC4124", "SVC6033");
		map.put("SVC4128", "SVC6033");
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", "SVC6033");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.SUBMIT_BLUEPRINT, map);
	}

	private void setGetRuleMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.GET_RULE_ARTIFACT, map);
	}

	private void setSaveRuleMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4063", "SVC6036");
		map.put("SVC4122", "SVC6036");
		map.put("SVC4124", "SVC6036");
		map.put("SVC4128", "SVC6036");
		map.put("SVC4125", AS_IS);
		map.put("SVC4127", "SVC6036");
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		map.put("SVC4000", "SVC6036");
		sdcDcaeMsgIdMap.put(ApiType.SAVE_RULE_ARTIFACT, map);
	}

	private void setGetAllVfcmtMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("SVC4642", "200");
		sdcDcaeMsgIdMap.put(ApiType.GET_ALL_VFCMTS, map);
	}


	private void setDeleteReferenceMapping(){
		Map<String, String> map = new HashMap<>();
		map.put("POL5003", AS_IS);
		map.put("SVC4063", AS_IS);
		map.put("POL4050", AS_IS);
		map.put("SVC4086", AS_IS);
		map.put("SVC4301", AS_IS);
		map.put("SVC4687", AS_IS);
		sdcDcaeMsgIdMap.put(ApiType.DELETE_VFCMT_REFERENCE, map);
	}

	private void populateSdcDcaeMsgIdMap() {
		sdcDcaeMsgIdMap = new EnumMap<>(ApiType.class);
		setAttachToServiceMapping();
		setCheckinResourceMapping();
		setCheckoutResourceMapping();
		setCloneVfcmtMapping();
		setGetAllVfcmtMapping();
		setGetRuleMapping();
		setCreateNewVfcmtMapping();
		setGetCdumpMapping();
		setGetModelMapping();
		setSaveCdumpMapping();
		setSaveRuleMapping();
		setSubmitBlueprintMapping();
		setGetServiceMapping();
		setGetVfcmtMapping();
		setSdcCatalogPolicyMapping();
		setDeleteReferenceMapping();
	}

	public enum ApiType {
		CREATE_NEW_VFCMT,
		GET_ALL_VFCMTS,
		CLONE_VFCMT,
		GET_VFCMT,
		GET_SERVICE,
		ATTACH_TO_SERVICE,
		GET_CDUMP,
		GET_MODEL,
		CHECK_OUT_RESOURCE,
		CHECK_IN_RESOURCE,
		SAVE_CDUMP,
		SUBMIT_BLUEPRINT,
		GET_RULE_ARTIFACT,
		SAVE_RULE_ARTIFACT,
		ALL_SDC_CATALOG,
		DELETE_VFCMT_REFERENCE
	}
	
	public ResponseFormat getResponseFormat(ActionStatus actionStatus, String notes, String... variables) {
		return responseFormatManager.getResponseFormat(actionStatus, notes, variables);
	}
	
	public ResponseEntity buildErrorResponse(ActionStatus actionStatus, String notes, String... variables) {
		ResponseFormat response = responseFormatManager.getResponseFormat(actionStatus, notes, variables);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
	}

	public ResponseEntity buildErrorResponse(ActionStatus actionStatus) {
		ResponseFormat response = responseFormatManager.getResponseFormat(actionStatus, "");
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
	}

	public ResponseEntity buildErrorResponse(BaseException baseException) {
		ResponseFormat response = responseFormatManager.getResponseFormat(baseException);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
	}

	public ResponseEntity buildErrorArrayResponse(List<ServiceException> errors) {
		ResponseFormat response = responseFormatManager.getResponseFormat(errors);
		return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
	}

	// ActionStatus determined by sdc to dcae mapping
	public ActionStatus convertToDcaeActionStatus(String messageId, ApiType apiType) {
		// try the apiType's specific mapping from SDC messageId to dcaeMessageId
		String dcaeMessageId = sdcDcaeMsgIdMap.get(apiType).get(messageId);
		// if no specific mapping found try the general mapping
		if(null == dcaeMessageId)
			dcaeMessageId = sdcDcaeMsgIdMap.get(ApiType.ALL_SDC_CATALOG).get(messageId);
		// if no mapping found return general error
		if(null == dcaeMessageId)
			return ActionStatus.GENERAL_ERROR;
		// if mapped to 'AS_IS' return 'AS_IS'
		if(AS_IS.equals(dcaeMessageId))
			return ActionStatus.AS_IS;
		// for any other valid mapping fetch the ActionStatus by corresponding dcaeMessageId
		return responseFormatManager.getMsgIdToActionStatusMap().get(dcaeMessageId);
	}

	public ResponseEntity handleException(Exception e, ApiType apiType, String... variables){
		if (e instanceof ASDCException){
			ASDCException se = (ASDCException)e;
			ActionStatus status = convertToDcaeActionStatus(se.getMessageId(), apiType);
			switch (status) {
			case AS_IS:
				return buildErrorResponse(se);
			case OK:
				return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
			default:
				return buildErrorResponse(status, se.getMessage(), variables);
			}
		}
		//TODO refactor - don't throw DcaeException
		if (e instanceof DcaeException){
			return buildErrorResponse((DcaeException)e);
		}
		return buildErrorResponse(ActionStatus.GENERAL_ERROR, e.getMessage());
	}
}

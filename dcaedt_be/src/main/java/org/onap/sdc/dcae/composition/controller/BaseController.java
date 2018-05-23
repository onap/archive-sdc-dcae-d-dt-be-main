package org.onap.sdc.dcae.composition.controller;

import javax.servlet.http.HttpServletRequest;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.impl.BaseBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.sdc.Asset;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.DcaeException;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.google.gson.Gson;

public abstract class BaseController {
	
	protected Gson gson = new Gson();

	@Autowired
	protected SystemProperties systemProperties;

	@Autowired
	protected BaseBusinessLogic baseBusinessLogic;

	protected OnapLoggerError errLogger = OnapLoggerError.getInstance();
	protected OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	@ModelAttribute("requestId")
	public String getRequestId(HttpServletRequest request) {
		return request.getAttribute("requestId").toString();
	}

	ResourceDetailed checkoutVfcmt(String userId, String uuid, String requestId) {
		return baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, uuid, LifecycleOperationType.CHECKOUT.name(), null, requestId);
	}

	ResourceDetailed checkinVfcmt(String userId, String uuid, String requestId) {
		return baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, uuid, LifecycleOperationType.CHECKIN.name(), "checking in vfcmt"  + uuid, requestId);
	}


	boolean isNeedToCheckOut(String lifecycleState) {
		return DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT != DcaeBeConstants.LifecycleStateEnum.findState(lifecycleState);
	}

	void checkUserIfResourceCheckedOut(String userId, Asset asset) {
		if (DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT == DcaeBeConstants.LifecycleStateEnum.findState(asset.getLifecycleState())) {
			String lastUpdaterUserId = asset.getLastUpdaterUserId();
			if (lastUpdaterUserId != null && !lastUpdaterUserId.equals(userId)) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "User conflicts. Operation not allowed for user {} on resource checked out by {}", userId, lastUpdaterUserId);
				ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.USER_CONFLICT, null, userId, asset.getName(), lastUpdaterUserId);
				throw new DcaeException(HttpStatus.FORBIDDEN, responseFormat.getRequestError());
			}
		}
	}

	void checkVfcmtType(ResourceDetailed vfcmt) {
		if (AssetType.VFCMT != getValidAssetTypeOrNull(vfcmt.getResourceType()) || !"Template".equals(vfcmt.getCategory())) {
			ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.RESOURCE_NOT_VFCMT_ERROR, null, vfcmt.getUuid());
			throw new DcaeException(HttpStatus.BAD_REQUEST, responseFormat.getRequestError());
		}
	}

	ResponseEntity handleException(Exception e, ApiType apiType, String... variables){
		errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
		return ErrConfMgr.INSTANCE.handleException(e, apiType, variables);
	}

	AssetType getValidAssetTypeOrNull(String type) {
		try {
			return AssetType.getAssetTypeByName(type);
		} catch (IllegalArgumentException e) {
			debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "invalid asset type: {}. Error: {}", type, e);
			return null;
		}
	}
}

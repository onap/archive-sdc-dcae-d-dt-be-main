package tools;
import com.google.gson.JsonObject;
import json.templateInfo.TemplateInfo;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import utilities.IDcaeRestClient;
import utilities.IReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class DeployTemplate {
    private static final String FAILED_UPDATE_VFCMT = "Failed update vfcmt: ";
    private static final String FAILED = "failed";
    private final IReport report;
    private final IDcaeRestClient dcaeRestClient;
    private LoggerError errLogger = LoggerError.getInstance();
    private LoggerDebug debugLogger = LoggerDebug.getInstance();

    DeployTemplate(IReport report, IDcaeRestClient dcaeRestClient) {

        this.report = report;
        this.dcaeRestClient = dcaeRestClient;
    }

    public void deploy(Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap) {
        ArrayList<ResourceDetailed> vfcmtList = new ArrayList<>();
        List<ResourceDetailed> regularVfcmtList = dcaeRestClient.getAllVfcmts();
        if (regularVfcmtList != null) {
            vfcmtList.addAll(regularVfcmtList);
        }
        List<ResourceDetailed> baseVfcmtList = dcaeRestClient.getAllBaseVfcmts();
        if (baseVfcmtList != null) {
            vfcmtList.addAll(baseVfcmtList);
        }

        List<TemplateInfo> updatedTemplateInfos = new ArrayList<>();
        vfcmtList.forEach(vfcmt ->
                templateInfoToJsonObjectMap.keySet().stream().filter(templateInfo -> templateInfo.getName().equalsIgnoreCase(vfcmt.getName())).forEach(templateInfo -> {
                    update(vfcmt, templateInfo, templateInfoToJsonObjectMap.get(templateInfo));
                    updatedTemplateInfos.add(templateInfo);
                }));
        templateInfoToJsonObjectMap.keySet().stream()
                .filter(templateInfo -> !updatedTemplateInfos.contains(templateInfo))
                .forEach(templateInfo -> createNew(templateInfo, templateInfoToJsonObjectMap.get(templateInfo)));

        verify(templateInfoToJsonObjectMap);
    }

    private void verify(Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap) {
        AtomicInteger foundCount = new AtomicInteger();
        debugLogger.log("Starting verify deployment");
        ArrayList<ResourceDetailed> vfcmtList = new ArrayList<>();
        List<ResourceDetailed> regularVfcmtList = dcaeRestClient.getAllVfcmts();
        if (regularVfcmtList != null) {
            vfcmtList.addAll(regularVfcmtList);
        }
        List<ResourceDetailed> baseVfcmtList = dcaeRestClient.getAllBaseVfcmts();
        if (baseVfcmtList != null) {
            vfcmtList.addAll(baseVfcmtList);
        }

        templateInfoToJsonObjectMap.keySet()
                .forEach(templateInfo -> vfcmtList.stream()
                        .filter(vfcmt -> vfcmt.getName().equalsIgnoreCase(templateInfo.getName()))
                        .forEach(vfcmt -> foundCount.getAndIncrement()));
        if (foundCount.get() == templateInfoToJsonObjectMap.size()) {
            debugLogger.log("Deployment verify finished successfully");
        } else {
            errLogger.log("Deployment verify finished successfully");
            String msg = "Deployment verify finished with errors, found only: " +
                    foundCount.get() + " of " + templateInfoToJsonObjectMap.size() + " vfcmts";
            report.addErrorMessage(msg);
            errLogger.log(msg);
        }
    }

    private void createNew(TemplateInfo templateInfo, JsonObject jsonObject) {
        try {
            CreateVFCMTRequest createVFCMTRequest = new CreateVFCMTRequest();
            createVFCMTRequest.setName(templateInfo.getName());
            createVFCMTRequest.setDescription(templateInfo.getDescription());
            createVFCMTRequest.setSubcategory(templateInfo.getSubCategory());
            createVFCMTRequest.setCategory(templateInfo.getCategory());
            ResourceDetailed vfcmt = dcaeRestClient.createResource(createVFCMTRequest);

            saveAndCertify(jsonObject, vfcmt);

        } catch (Exception e) {
            String msg = FAILED_UPDATE_VFCMT + templateInfo.getName() + ", With general message: " + e.getMessage();
            report.addErrorMessage(msg);
            errLogger.log(msg + " " + e);
            report.setStatusCode(2);
        }
    }

    private void update(ResourceDetailed vfcmt, TemplateInfo templateInfo, JsonObject jsonObject) {
        ResourceDetailed checkedoutVfcmt = vfcmt;
        try {
            boolean vfcmtIsCheckedOut = isCheckedOut(vfcmt);
            if (vfcmtIsCheckedOut && differentUserCannotCheckout(dcaeRestClient.getUserId(), vfcmt)){
                report.addErrorMessage(FAILED_UPDATE_VFCMT + vfcmt.getName() + ", cannot checkout vfcmt");
                return;
            }
            if (templateInfo.getUpdateIfExist()) {
                if (!vfcmtIsCheckedOut) {
                    checkedoutVfcmt = dcaeRestClient.checkoutVfcmt(vfcmt.getUuid());
                }
                if (checkedoutVfcmt != null) {
                    checkedoutVfcmt.setSubCategory(templateInfo.getSubCategory());
                    checkedoutVfcmt.setCategory(templateInfo.getCategory());
                    checkedoutVfcmt.setDescription(templateInfo.getDescription());
                    dcaeRestClient.updateResource(checkedoutVfcmt);
                    saveAndCertify(jsonObject, checkedoutVfcmt);
                }
            } else {
                report.addNotUpdatedMessage("vfcmt: " + vfcmt.getName() + " found, but didn't update.");
            }
        } catch (Exception e) {
            String msg = FAILED_UPDATE_VFCMT + vfcmt.getName() + ", With general message: " + e.getMessage();
            report.addErrorMessage(msg);
            errLogger.log( msg + " " + e);
			report.setStatusCode(2);
        }
    }

    private void saveAndCertify(JsonObject jsonObject, ResourceDetailed checkedoutVfcmt) {
		jsonObject.addProperty("cid", checkedoutVfcmt.getUuid());
        if (saveCompositionAndCertify(checkedoutVfcmt, jsonObject)) {
            report.addUpdatedMessage("vfcmt: " + checkedoutVfcmt.getName() + " updated successfully");
        } else {
            report.addErrorMessage("VFCMT " + checkedoutVfcmt.getName() + " failed to update");
        }
    }

    private boolean saveCompositionAndCertify(ResourceDetailed vfcmt, JsonObject jsonObject) {
        if (vfcmt.getUuid() == null) {
            return false;
        }

        debugLogger.log("Saving cdump of: " + vfcmt.getName() + " vfcmt");
        debugLogger.log(jsonObject.toString());

        String responseEntity = dcaeRestClient.saveComposition(vfcmt.getUuid(), jsonObject.toString());
        if (responseEntity.equalsIgnoreCase(FAILED)) {
            String msg = "Failed saving vfcmt: " + vfcmt.getName();
            report.addErrorMessage(msg);
            errLogger.log(msg);
            return false;
        }
        dcaeRestClient.certifyVfcmt(vfcmt.getUuid());
        return true;
    }

    private boolean isCheckedOut(ResourceDetailed asset) {
        return DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT == DcaeBeConstants.LifecycleStateEnum.findState(asset.getLifecycleState());
    }

    private Boolean differentUserCannotCheckout(String userId, ResourceDetailed asset) {
        String lastUpdaterUserId = asset.getLastUpdaterUserId();
        if (lastUpdaterUserId != null && !lastUpdaterUserId.equals(userId)) {
            String msg = "User conflicts. Operation not allowed for user "+userId+" on resource checked out by "+lastUpdaterUserId;
            report.addErrorMessage(msg);
            errLogger.log(msg);
            return true;
        } else {
            return false;
        }
    }
}

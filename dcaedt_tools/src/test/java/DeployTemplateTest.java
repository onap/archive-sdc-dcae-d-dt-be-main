import com.google.gson.JsonObject;
import json.templateInfo.TemplateInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import tools.DeployTemplate;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeployTemplateTest extends BaseTest {

    @InjectMocks
    DeployTemplate deployTemplate;

    private Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap;

    @Before
    @Override
    public void setup() {
        super.setup();
        super.mockGetAllVfcmt();
        super.mockCheckoutVfcmtAndCreateResource();
        when(dcaeRestClient.getUserId()).thenReturn(USER_ID);
        when(dcaeRestClient.saveComposition(any(), any())).thenReturn("Composition Created");

        templateInfoToJsonObjectMap = new HashMap<>();
        TemplateInfo templateInfo = new TemplateInfo();
        templateInfo.setName(VFCMT_NAME1);
        templateInfo.setFlowType(TEMPLATE_INFO_FLOWTYPE);
        templateInfo.setCategory("category");
        templateInfo.setSubCategory("subCategory");
        templateInfo.setDescription("description");
        templateInfo.setUpdateIfExist(true);
        templateInfoToJsonObjectMap.put(templateInfo, new JsonObject());
        templateInfo = new TemplateInfo();
        templateInfo.setName(TEMPLATE_INFO_NAME);
        templateInfo.setFlowType(TEMPLATE_INFO_FLOWTYPE);
        templateInfo.setCategory("category");
        templateInfo.setSubCategory("subCategory");
        templateInfo.setDescription("description");
        templateInfoToJsonObjectMap.put(templateInfo, new JsonObject());
    }

    @Test
    public void deployHappyFlow() {
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(0)).addErrorMessage(anyString());
    }

    @Test
    public void deploy_failedSaving_failedVerify() {
        when(dcaeRestClient.saveComposition(anyString(), anyString())).thenReturn("failed");
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(4)).addErrorMessage(anyString());
    }
}

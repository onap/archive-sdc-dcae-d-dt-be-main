import json.response.ElementsResponse.Element;
import json.response.ItemsResponse.Item;
import json.response.ItemsResponse.Model;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import utilities.IDcaeRestClient;
import utilities.IReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
abstract class BaseTest {
    static final String USER_ID = "userId";
    static final String TEMPLATE_INFO_NAME = "templateInfoName";
    static final String VFCMT_NAME1 = "my vfcmt1";
    static final String UUID1 = "my uuid1";
    static final String VFCMT_NAME2 = "my vfcmt2";
    static final String UUID2 = "my uuid2";
    static final String VFCMT_NAME3 = "my vfcmt3";
    static final String UUID3 = "my uuid3";
    static final String ELEMENT_NAME1 = "my element1";
    static final String ELEMENT_NAME2 = "my element2";
    static final String ELEMENT_NAME3 = "my element3";
    static final String ALIAS_NAME1 = "my alias1";
    static final String ALIAS_NAME2 = "my alias2";
    static final String ALIAS_NAME3 = "my alias3";
    static final String ITEM_NAME1 = "my item1";
    static final String ITEM_NAME2 = "my item2";
    static final String ITEM_NAME3 = "my item3";

    @Mock
    IReport report;
    @Mock
    IDcaeRestClient dcaeRestClient;

    @Before
    public void setup() {
        when(dcaeRestClient.getUserId()).thenReturn(USER_ID);
        mockGetAllVfcmt();
        mockGetElements();
        mockGetItems();
        mockGetItemModel();
        mockGetItemType();
        mockCheckoutVfcmtAndCreateResource();
        when(dcaeRestClient.saveComposition(anyString(), anyString())).thenReturn("Composition Created");
    }

    private void mockCheckoutVfcmtAndCreateResource() {
        ResourceDetailed resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME1);
        resourceDetailed.setUuid(UUID1);
        resourceDetailed.setLifecycleState("NOT_CERTIFIED_CHECKOUT");
        resourceDetailed.setLastUpdaterUserId(USER_ID);
        when(dcaeRestClient.checkoutVfcmt(anyString())).thenReturn(resourceDetailed);
        when(dcaeRestClient.createResource(any())).thenReturn(resourceDetailed);
    }

    private void mockGetItemType() {
        when(dcaeRestClient.getItemType(anyString(), anyString())).thenReturn("{\"data\":{\"type\":{\"itemId\":\"e45ec9d7-01df-4cb1-896f-aff2a6ca5a8b/tosca.dcae.nodes.cdapApp.Map\", \"typeinfo\":\"typeInfo\"}}}");
    }

    private void mockGetItemModel() {
        when(dcaeRestClient.getItemModel(anyString())).thenReturn("{\"data\":{\"model\":{\"nodes\":[{\"capability\":{\"type\":\"someType\"}, \"type\":\"type\", \"name\":\"SomeNameFromRequirement\", \"requirements\":[{\"name\":\"SomeNameFromRequirement\"}], \"properties\":[{}], \"capabilities\":[{\"name\":\"SomeNameToCapability\"}],\"type\":\"type\"}]}}}",
                "{\"data\":{\"model\":{\"nodes\":[{\"capability\":{\"type\":\"someType\"}, \"type\":\"type\", \"name\":\"SomeNameToCapability\", \"requirements\":[{\"name\":\"SomeNameFromRequirement\"}], \"properties\":[{}], \"capabilities\":[{\"name\":\"SomeNameToCapability\"}],\"type\":\"type\"}]}}}");
    }

    private void mockGetItems() {
        when(dcaeRestClient.getItem(ELEMENT_NAME1)).thenReturn(null);
        List<Item> items = new ArrayList<>();
        Item item = new Item();
        item.setName(ITEM_NAME1);
        Model model =  new Model();
        model.setItemId("");
        List<Model> models = Collections.singletonList(model);
        item.setModels(models);
        items.add(item);
        item = new Item();
        item.setName(ITEM_NAME2);
        item.setModels(models);
        items.add(item);
        when(dcaeRestClient.getItem(ELEMENT_NAME2)).thenReturn(items);
        items = new ArrayList<>();
        item = new Item();
        item.setName(ITEM_NAME3);
        item.setModels(models);
        items.add(item);
        when(dcaeRestClient.getItem(ELEMENT_NAME3)).thenReturn(items);
    }

    private void mockGetElements() {
        List<Element> elements = new ArrayList<>();
        Element element = new Element();
        element.setName(ELEMENT_NAME1);
        elements.add(element);
        element = new Element();
        element.setName(ELEMENT_NAME2);
        elements.add(element);
        element = new Element();
        element.setName(ELEMENT_NAME3);
        elements.add(element);
        when(dcaeRestClient.getElements()).thenReturn(elements);
    }

    private void mockGetAllVfcmt() {
        List<ResourceDetailed> resourceDetaileds = new ArrayList<>();
        ResourceDetailed resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME1);
        resourceDetailed.setUuid(UUID1);
        resourceDetailed.setLifecycleState("NOT_CERTIFIED_CHECKOUT");
        resourceDetailed.setLastUpdaterUserId(USER_ID);
        resourceDetaileds.add(resourceDetailed);
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME2);
        resourceDetailed.setUuid(UUID2);
        resourceDetaileds.add(resourceDetailed);
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME3);
        resourceDetailed.setUuid(UUID3);
        resourceDetaileds.add(resourceDetailed);

        List<ResourceDetailed> resourceDetaileds2 = new ArrayList<>();
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME1);
        resourceDetailed.setUuid(UUID1);
        resourceDetailed.setLifecycleState("NOT_CERTIFIED_CHECKOUT");
        resourceDetailed.setLastUpdaterUserId(USER_ID);
        resourceDetaileds2.add(resourceDetailed);
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME2);
        resourceDetailed.setUuid(UUID2);
        resourceDetaileds2.add(resourceDetailed);
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME3);
        resourceDetailed.setUuid(UUID3);
        resourceDetaileds2.add(resourceDetailed);
        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(TEMPLATE_INFO_NAME);
        resourceDetailed.setUuid(UUID3);
        resourceDetaileds2.add(resourceDetailed);
        when(dcaeRestClient.getAllVfcmts()).thenReturn(resourceDetaileds, resourceDetaileds2);
    }
}

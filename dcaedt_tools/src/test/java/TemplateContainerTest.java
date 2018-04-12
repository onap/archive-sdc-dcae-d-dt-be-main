import com.google.gson.JsonObject;
import json.response.ItemsResponse.Item;
import json.response.ItemsResponse.Model;
import json.templateInfo.Composition;
import json.templateInfo.Relation;
import json.templateInfo.TemplateInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tools.TemplateContainer;

import java.util.*;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TemplateContainerTest extends BaseTest {

    private TemplateContainer templateContainer;

    private List<TemplateInfo> templateInfos;
    private Map<String, List<Item>> elementsByFolderNames;
    @Before
    @Override
    public void setup() {
        super.setup();
        templateInfos = new ArrayList<>();
        TemplateInfo templateInfo = new TemplateInfo();
        templateInfo.setName(TEMPLATE_INFO_NAME);
        Composition composition = new Composition();
        composition.setType(ELEMENT_NAME3);
        composition.setAlias(ALIAS_NAME3);
        templateInfo.setComposition(Collections.singletonList(composition));
        templateInfos.add(templateInfo);
        elementsByFolderNames = new HashMap<>();
        Item item = new Item();
        item.setName(ELEMENT_NAME3);
        item.setItemId("");
        Model model =  new Model();
        model.setItemId("");
        List<Model> models = Collections.singletonList(model);
        item.setModels(models);
        List<Item> items = new ArrayList<>(Collections.singletonList(item));
        elementsByFolderNames.put(TEMPLATE_INFO_NAME, items);

    }

    @Test
    public void getCdumps_emptyTemplateInfo_returnEmptyMap() {
        templateContainer = new TemplateContainer(report, dcaeRestClient, new ArrayList<>(), new HashMap<>());

        Map<TemplateInfo, JsonObject> templateInfoJsonObjectMap = templateContainer.getCdumps();

        Assert.assertTrue(templateInfoJsonObjectMap.size() == 0);
    }

    @Test
    public void getCdumps_returnNotFoundEmptyList() {
        elementsByFolderNames = new HashMap<>();
        Item item = new Item();
        item.setName(ELEMENT_NAME2);
        List<Item> items = new ArrayList<>(Collections.singletonList(item));
        elementsByFolderNames.put(TEMPLATE_INFO_NAME, items);
        templateContainer = new TemplateContainer(report, dcaeRestClient, templateInfos, elementsByFolderNames);

        Map<TemplateInfo, JsonObject> templateInfoJsonObjectMap = templateContainer.getCdumps();

        verify(report).addErrorMessage(anyString());
        Assert.assertTrue(templateInfoJsonObjectMap.size() == 0);
    }

    @Test
    public void getCdumps_returnOneCdump() {
        templateContainer = new TemplateContainer(report, dcaeRestClient, templateInfos, elementsByFolderNames);

        Map<TemplateInfo, JsonObject> templateInfoJsonObjectMap = templateContainer.getCdumps();
        JsonObject jsonObject = templateInfoJsonObjectMap.get(templateInfos.get(0));
        String result = jsonObject.toString();

        verifyCdump(result);
        verify(report, times(0)).addErrorMessage(anyString());
        Assert.assertTrue(templateInfoJsonObjectMap.size() == 1);
    }

    @Test
    public void getChumps_returnOneChumpWithRelations() {
        templateInfos = new ArrayList<>();
        TemplateInfo templateInfo = new TemplateInfo();
        templateInfo.setName(TEMPLATE_INFO_NAME);
        List<Composition> compositionList = new ArrayList<>();
        Composition composition = new Composition();
        composition.setType(ELEMENT_NAME3);
        composition.setAlias(ALIAS_NAME3);
        compositionList.add(composition);
        composition = new Composition();
        composition.setType(ELEMENT_NAME2);
        composition.setAlias(ALIAS_NAME2);
        compositionList.add(composition);
        templateInfo.setComposition(compositionList);
        Relation relation = new Relation();
        relation.setFromComponent(ALIAS_NAME3 + ".SomeNameFromRequirement");
        relation.setToComponent(ALIAS_NAME2 + ".SomeNameToCapability");
        relation.setFromRequirement("SomeNameFromRequirement");
        relation.setToCapability("SomeNameToCapability");
        templateInfo.setRelations(Collections.singletonList(relation));
        templateInfos.add(templateInfo);
        elementsByFolderNames = new HashMap<>();
        List<Item> itemList = new ArrayList<>();
        Item item = new Item();
        item.setName(ELEMENT_NAME3);
        item.setItemId("");
        Model model =  new Model();
        model.setItemId("");
        List<Model> models = Collections.singletonList(model);
        item.setModels(models);
        itemList.add(item);
        item = new Item();
        item.setName(ELEMENT_NAME2);
        item.setItemId("");
        model =  new Model();
        model.setItemId("");
        models = Collections.singletonList(model);
        item.setModels(models);
        itemList.add(item);
        elementsByFolderNames.put(TEMPLATE_INFO_NAME, itemList);
        templateContainer = new TemplateContainer(report, dcaeRestClient, templateInfos, elementsByFolderNames);

        Map<TemplateInfo, JsonObject> templateInfoJsonObjectMap = templateContainer.getCdumps();
        JsonObject jsonObject = templateInfoJsonObjectMap.get(templateInfos.get(0));
        String result = jsonObject.toString();

        verifyCdumpRelations(result);
        verify(report, times(0)).addErrorMessage(anyString());
        Assert.assertTrue(templateInfoJsonObjectMap.size() == 1);
    }

    private void verifyCdumpRelations(String result) {
        Assert.assertTrue(result.contains("p2\":\"SomeNameToCapability\""));
        Assert.assertTrue(result.contains("\"SomeNameFromRequirement\",null,\"SomeNameToCapability\""));
        Assert.assertTrue(result.contains("\"relationship\":["));
        Assert.assertTrue(result.contains("\"n1\":\"n."));
        Assert.assertTrue(result.contains("\"relations\":[{"));
        Assert.assertTrue(result.contains("\"name1\":\"SomeNameFromRequirement\""));
        Assert.assertTrue(result.contains("\"n2\":\"n."));
        Assert.assertTrue(result.contains("\"p1\":\"SomeNameFromRequirement\""));
    }

    private void verifyCdump(String result) {
        String expectedResultStart = "{\"version\":0,\"flowType\":\"templateInfoName\",\"nodes\":[{\"name\":\"SomeNameFromRequirement\",\"description\":\"\",\"id\":\"e45ec9d7-01df-4cb1-896f-aff2a6ca5a8b\",\"nid\":\"n.";
        String expectedResultMid =  "\",\"capabilities\":[{\"name\":\"SomeNameToCapability\"}],\"requirements\":[{\"name\":\"SomeNameFromRequirement\"}],\"properties\":[{}],\"typeinfo\":{\"itemId\":\"e45ec9d7-01df-4cb1-896f-aff2a6ca5a8b/tosca.dcae.nodes.cdapApp.Map\",\"typeinfo\":\"typeInfo\"},\"type\":{\"name\":\"type\"},\"ndata\":{\"name\":\"n.";
        String expectedResultEnd = "\",\"label\":\"SomeNameFromRequirement\",\"x\":438,\"y\":435,\"px\":437,\"py\":434,\"ports\":[],\"radius\":30}}],\"inputs\":[],\"outputs\":[],\"relations\":[]}";
        Assert.assertTrue(result.startsWith(expectedResultStart));
        Assert.assertTrue(result.contains(expectedResultMid));
        Assert.assertTrue(result.endsWith(expectedResultEnd));
    }
}

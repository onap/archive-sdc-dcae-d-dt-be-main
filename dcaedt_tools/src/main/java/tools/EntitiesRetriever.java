package tools;

import json.response.ElementsResponse.Element;
import json.response.ItemsResponse.Item;
import utilities.IDcaeRestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EntitiesRetriever {
    private final IDcaeRestClient dcaeRestClient;

    EntitiesRetriever(IDcaeRestClient dcaeRestClient) {

        this.dcaeRestClient = dcaeRestClient;
    }

    public Map<String, List<Item>> getElementsByFolder() {
        List<Element> elementList = dcaeRestClient.getElements();
        Map<String, List<Item>> elementsByFolderNames = new HashMap<>();

        for (Element element : elementList) {
            List<Item> items = dcaeRestClient.getItem(element.getName());
            if (items == null) {
                continue;
            }
            elementsByFolderNames.put(element.getName(), items);
        }
        return elementsByFolderNames;
    }
}

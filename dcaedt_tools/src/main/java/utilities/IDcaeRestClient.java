package utilities;

import json.Environment;
import json.response.ElementsResponse.Element;
import json.response.ItemsResponse.Item;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;

import java.util.List;

public interface IDcaeRestClient {
    void init(Environment environment);

    String getUserId();

    List<ResourceDetailed> getAllVfcmts();

    List<ResourceDetailed> getAllBaseVfcmts();

    ResourceDetailed createResource(CreateVFCMTRequest resource);

    ResourceDetailed checkoutVfcmt(String vfcmtUuid);

    ResourceDetailed checkinVfcmt(String vfcmtUuid);

    List<Element> getElements();

    List<Item> getItem(String element);

    String getItemModel(String elementId);

    String getItemType(String elementId, String type);

    String saveComposition(String componentId, String body);

    String certifyVfcmt(String vfcmtUuid);

    void updateResource(ResourceDetailed vfcmt);
}

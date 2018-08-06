package utilities;

import json.Environment;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;

import java.util.List;
import java.util.Map;

public interface IDcaeRestClient {
    void init(Environment environment);

    String getUserId();

    List<ResourceDetailed> getAllVfcmts();

    List<ResourceDetailed> getAllBaseVfcmts();

    ResourceDetailed createResource(CreateVFCMTRequest resource);

    ResourceDetailed checkoutVfcmt(String vfcmtUuid);

    ResourceDetailed checkinVfcmt(String vfcmtUuid);

	Map<String, List<Resource>> getDcaeCatalog();

    String getItemModel(String elementId);

    String getItemType(String elementId, String type);

    String saveComposition(String componentId, String body);

    String certifyVfcmt(String vfcmtUuid);

    void updateResource(ResourceDetailed vfcmt);
}

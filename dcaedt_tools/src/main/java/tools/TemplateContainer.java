package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import json.templateInfo.Composition;
import json.templateInfo.NodeToDelete;
import json.templateInfo.Relation;
import json.templateInfo.TemplateInfo;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import utilities.IDcaeRestClient;
import utilities.IReport;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class TemplateContainer {
    private static final String NODES = "nodes";
    private static final String RELATIONSHIP = "relationship";
    private static final String ASSIGNMENT = "assignment";
    private static long nidCounter = 0;
    private final IReport report;
    private final IDcaeRestClient dcaeRestClient;
    private final List<TemplateInfo> templateInfos;
    private final Map<String, List<Resource>> elementsByFolderNames;
    private LoggerDebug debugLogger = LoggerDebug.getInstance();


    public TemplateContainer(IReport report, IDcaeRestClient dcaeRestClient, List<TemplateInfo> templateInfos, Map<String, List<Resource>> elementsByFolderNames) {
        this.report = report;
        this.dcaeRestClient = dcaeRestClient;
        this.templateInfos = templateInfos;
        this.elementsByFolderNames = elementsByFolderNames;
    }

    private List<ItemAndAlias> findTemplate(TemplateInfo templateInfo) {
        AtomicReference<List<ItemAndAlias>> items = new AtomicReference<>();
        items.set(new ArrayList<>());
        elementsByFolderNames.keySet().stream()
                .forEach(folderName -> {
                    List<ItemAndAlias> itemList = returnMatchedTemplate(folderName, templateInfo);
                    items.get().addAll(itemList);
                });
        if (items.get().size() == templateInfo.getComposition().size()) {
            return items.get();
        }
        return new ArrayList<>();
    }

    private List<ItemAndAlias> returnMatchedTemplate(String folderName, TemplateInfo templateInfo) {
        List<ItemAndAlias> items = new ArrayList<>();
        elementsByFolderNames.get(folderName).stream()
                .forEach(item -> templateInfo.getComposition().stream().forEach(composition ->
                {
                    if (composition.getType().equalsIgnoreCase(item.getName())) {
                        items.add(new ItemAndAlias(item, composition.getAlias()));
                    }
                }));
        return items;
    }


    public Map<TemplateInfo, JsonObject> getCdumps() {
        Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap = new HashMap<>();
        for (TemplateInfo templateInfo : templateInfos) {
            List<ItemAndAlias> items = findTemplate(templateInfo);
            if (items == null || items.isEmpty()) {
                report.addErrorMessage("vfcmt: " + templateInfo.getName() + ". DCAE Component not found");
                report.setStatusCode(1);
                continue;
            }
            templateInfoToJsonObjectMap.put(templateInfo, getCdumpJsonObject(items, templateInfo));
        }
        return templateInfoToJsonObjectMap;
    }

    private JsonObject getCdumpJsonObject(List<ItemAndAlias> ItemsAndAlias, TemplateInfo templateInfo) {
        JsonObject cdumpJsonObject = generateCdumpInput(templateInfo);
        Map<ItemAndAlias, Map<String, NodeData>> itemMapHashMap = new HashMap<>();
        JsonArray relationsJsonArray = new JsonArray();
        for (ItemAndAlias itemAndAlias : ItemsAndAlias) {
            Resource item = itemAndAlias.getItem();
            debugLogger.log("Creating cdump for item: " + item.getName());
            JsonArray jsonArrayNode = cdumpJsonObject.getAsJsonArray(NODES);
            JsonParser jsonParser = new JsonParser();
            JsonObject modelResponse = jsonParser.parse(dcaeRestClient.getItemModel(item.getUuid())).getAsJsonObject().get("data").getAsJsonObject().get("model").getAsJsonObject();
            JsonArray allNodeTemplates = modelResponse.get(NODES).getAsJsonArray();
            Map<String, NodeData> stringRelationsDataMap = new HashMap<>();
            for (JsonElement nodeElement : allNodeTemplates) {
                if (checkIfNeedToSkip(templateInfo.getNodesToDelete(), nodeElement, item.getName())) {
                    continue;
                }
                JsonObject responseModelJson = nodeElement.getAsJsonObject();
                JsonObject responseTypeInfoJson = jsonParser.parse(dcaeRestClient.getItemType(item.getUuid(), responseModelJson.get("type").getAsString())).getAsJsonObject().get("data").getAsJsonObject().get("type").getAsJsonObject();
                String nodeName = itemAndAlias.getAlias() != "" ? itemAndAlias.getAlias() + "." + responseModelJson.get("name").getAsString() : responseModelJson.get("name").getAsString();
                JsonObject jsonObjectElement = newNodeTemplate(nodeName, modelResponse.get("itemId").getAsString());
                jsonObjectElement.addProperty("id", responseTypeInfoJson.get("itemId").getAsString().split("/")[0]);
                String nid = "n." + new Date().getTime() + "." + nidCounter++;
                jsonObjectElement.addProperty("nid", nid);
                NodeData nodeData = createNodeData(responseModelJson, responseTypeInfoJson, responseModelJson.get("name").getAsString(), itemAndAlias.getAlias());
                fillPropertiesValue(nodeData);
                stringRelationsDataMap.put(nid, nodeData);
                addCdumpData(responseModelJson, jsonObjectElement, nid, nodeData);
                jsonArrayNode.add(jsonObjectElement);
            }
            itemMapHashMap.put(itemAndAlias, stringRelationsDataMap);
        }
        JsonElement jsonElement = createTemplateInfoRelations(templateInfo, itemMapHashMap);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                relationsJsonArray.add(element);
            }
        }
        jsonElement = createSelfRelations(itemMapHashMap);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                relationsJsonArray.add(element);
            }

        }
        cdumpJsonObject.add("relations", relationsJsonArray);
        return cdumpJsonObject;
    }

    private void addCdumpData(JsonObject responseModelJson, JsonObject jsonObjectElement, String nid, NodeData nodeData) {
        jsonObjectElement.add("capabilities", nodeData.getCapabilities());
        jsonObjectElement.add("requirements", nodeData.getRequirements());
        jsonObjectElement.add("properties", nodeData.getProperties());
        jsonObjectElement.add("typeinfo", nodeData.getTypeInfo());
        JsonObject typeJsonObject = new JsonObject();
        typeJsonObject.addProperty("name", responseModelJson.get("type").getAsString());
        jsonObjectElement.add("type", typeJsonObject);
        JsonElement ndataElement = createNData(responseModelJson.get("name").getAsString(), nid);
        jsonObjectElement.add("ndata", ndataElement);
    }

    private void fillPropertiesValue(NodeData nodeData) {
        for (Iterator<JsonElement> iterator = nodeData.getProperties().iterator(); iterator.hasNext(); ) {
            JsonElement property = iterator.next();
            if (!property.isJsonObject()) {
                continue;
            }
            if (property.getAsJsonObject().has("value")) {
                continue;
            }
            JsonElement jsonElement = new JsonObject();
            if (property.getAsJsonObject().has(ASSIGNMENT) &&
                    property.getAsJsonObject().get(ASSIGNMENT).getAsJsonObject().has("value")) {
                jsonElement = property.getAsJsonObject().get(ASSIGNMENT).getAsJsonObject().get("value");
            } else if (property.getAsJsonObject().has("default")) {
                jsonElement = property.getAsJsonObject().get("default");
            } else if (property.getAsJsonObject().has(ASSIGNMENT) &&
                    property.getAsJsonObject().get(ASSIGNMENT).getAsJsonObject().has("input") &&
                    property.getAsJsonObject().get(ASSIGNMENT).getAsJsonObject().get("input").getAsJsonObject().has("default")) {
                jsonElement = property.getAsJsonObject().get(ASSIGNMENT).getAsJsonObject().get("input").getAsJsonObject().get("default");
            }
            property.getAsJsonObject().add("value", jsonElement);
        }

    }

    private boolean checkIfNeedToSkip(List<NodeToDelete> nodesToDelete, JsonElement nodeElement, String itemName) {
        return nodesToDelete != null && nodesToDelete.stream().anyMatch(nodeToDelete -> {
            if (nodeToDelete.getType().equalsIgnoreCase(itemName)) {
                String nodeName = nodeElement.getAsJsonObject().get("name").toString().replace("\"", "");
                if (nodeToDelete.getNodeName().equalsIgnoreCase(nodeName)) {
                    debugLogger.log("Skipping node: " + nodeToDelete.getNodeName() + ", Item name: " + itemName);
                    return true;
                }
            }
            return false;
        });
    }

    //We need it only for printing the relations (front end requirement)
    private JsonElement createNData(String name, String nid) {
        JsonObject ndataElement = new JsonObject();
        ndataElement.addProperty("name", nid);
        ndataElement.addProperty("label", name);
        ndataElement.addProperty("x",438);
        ndataElement.addProperty("y",435);
        ndataElement.addProperty("px",437);
        ndataElement.addProperty("py",434);
        ndataElement.add("ports", new JsonArray());
        ndataElement.addProperty("radius", 30);

        return ndataElement;
    }

    private JsonElement createSelfRelations(Map<ItemAndAlias, Map<String, NodeData>> nodeDataByNidByItem) {
        JsonArray jsonArrayRelations = new JsonArray();
        for (ItemAndAlias item : nodeDataByNidByItem.keySet()) {
            Map<String, NodeData> nodeDataByNid = nodeDataByNidByItem.get(item);
            if (nodeDataByNid.size() < 2) {
                continue;
            }
            Map<JsonObject, String> nidListByRequirement = new HashMap<>();
            for (String nid : nodeDataByNid.keySet()) {
                JsonArray jsonArrayRequirements = nodeDataByNid.get(nid).getRequirements();
                for (JsonElement requirement : jsonArrayRequirements) {
                    JsonObject jsonObject = requirement.getAsJsonObject();
                    if (jsonObject.has("node")) {
                        nidListByRequirement.put(jsonObject, nid);
                    }
                }
            }
            for (JsonObject requirement : nidListByRequirement.keySet()) {
                String toNodeName = requirement.get("node").toString().replaceAll("\"", "");
                boolean isFound = false;
                NodeData toNodeData;
                String toNId = null;
                for (String nid : nodeDataByNid.keySet()) {
                    toNodeData = nodeDataByNid.get(nid);
                    toNId = nid;
                    if (toNodeData.getName().equalsIgnoreCase(toNodeName)) {
                        isFound = true;
                        break;
                    }
                }
                if (isFound) {
                    JsonObject relationElement = new JsonObject();
                    NodeData fromNode = nodeDataByNidByItem.get(item).get(nidListByRequirement.get(requirement));
                    relationElement.addProperty("rid", "ink." + nidListByRequirement.get(requirement) + "." + nidCounter++);
                    relationElement.addProperty("n1", nidListByRequirement.get(requirement));
                    relationElement.addProperty("name1", fromNode.getNameWithAlias());
                    JsonObject metaData = new JsonObject();
                    metaData.addProperty("n1", nidListByRequirement.get(requirement));
                    metaData.addProperty("p1", requirement.get("name").toString().replaceAll("\"", ""));
                    relationElement.addProperty("n2", toNId);
                    relationElement.addProperty("name2", StringUtils.isBlank(fromNode.getAliasBelong()) ? toNodeName : fromNode.getAliasBelong() + "." + toNodeName);
                    metaData.addProperty("n2", toNId);
                    String capabilityFullName = requirement.get("capability").getAsJsonObject().get("name").toString();
                    String capabilityShortName = StringUtils.substringAfterLast(capabilityFullName, ".");
                    metaData.addProperty("p2", capabilityShortName.replaceAll("\"", ""));
                    JsonArray relationship = new JsonArray();
                    relationship.add(fromNode.getName().replaceAll("\"", ""));
                    JsonElement requirementRelationship = requirement.get(RELATIONSHIP);
                    if (requirementRelationship != null) {
                        relationship.add(requirementRelationship.getAsJsonObject().get("type").toString().replaceAll("\"", ""));
                    } else {
                        relationship.add((JsonElement) null);
                    }

                    relationship.add(requirement.get("name").toString().replaceAll("\"", ""));
                    metaData.add(RELATIONSHIP, relationship);
                    relationElement.add("meta", metaData);
                    jsonArrayRelations.add(relationElement);
                }
            }
        }
        return jsonArrayRelations;
    }

    private NodeData createNodeData(JsonObject responseModelJson, JsonObject responseTypeInfoJson, String nodeName, String aliasBelong) {
        JsonArray capabilities = responseModelJson.get("capabilities").getAsJsonArray();
        JsonArray requirements = responseModelJson.get("requirements").getAsJsonArray();
        JsonArray properties = responseModelJson.get("properties").getAsJsonArray();
        return new NodeData(capabilities, requirements, properties, responseTypeInfoJson, nodeName, aliasBelong);
    }

    private JsonArray createTemplateInfoRelations(TemplateInfo templateInfo, Map<ItemAndAlias, Map<String, NodeData>> nodeDataByNidByItem) {
        JsonArray jsonArrayRelations = new JsonArray();

        if (templateInfo.getRelations() == null) {
            return null;
        }
        for (Relation relation : templateInfo.getRelations()) {
            JsonObject metaData = new JsonObject();
            JsonObject relationElement = new JsonObject();
            String fromComponent = relation.getFromComponent();
            String toComponent = relation.getToComponent();
            String fromComponentAlias = "";
            String fromComponentNodeName = fromComponent;
            if ((fromComponent.contains("."))) {
                fromComponentAlias = StringUtils.substringBefore(fromComponent, ".");
                fromComponentNodeName = StringUtils.substringAfterLast(fromComponent, ".");
            }
            String toComponentAlias = "";
            String toComponentNodeName = toComponent;
            if (toComponent.contains(".")) {
                toComponentAlias = StringUtils.substringBefore(toComponent, ".");
                toComponentNodeName = StringUtils.substringAfterLast(toComponent, ".");
            }
            boolean findTo = false;
            boolean findFrom = false;
            for (ItemAndAlias item : nodeDataByNidByItem.keySet()) {
                Map<String, NodeData> nodeDataByNid = nodeDataByNidByItem.get(item);
                for (String nid : nodeDataByNid.keySet()) {
                    NodeData currentNodeData = nodeDataByNid.get(nid);

                    String finalFromComponentAlias = fromComponentAlias;
                    String finalFromComponentNodeName = fromComponentNodeName;
                    Optional<Composition> isFoundComposition = templateInfo.getComposition().stream()
                            .filter(element -> finalFromComponentAlias.equalsIgnoreCase(element.getAlias())
                                    && element.getAlias().equalsIgnoreCase(currentNodeData.getAliasBelong())
                                    && element.getAlias().equalsIgnoreCase(item.getAlias())
                                    && element.getType().equalsIgnoreCase(item.getItem().getName())
                                    && finalFromComponentNodeName.equalsIgnoreCase(currentNodeData.getName())).findAny();
                    if (isFoundComposition.isPresent()) {
                        boolean isFound = findNode(relation.getFromRequirement(), currentNodeData.getRequirements());
                        if (isFound) {
                            if (findFrom) {
                                report.addErrorMessage("Found 2 match nodes, using the second one. from relation: " + relation.getFromRequirement());
                            }
                            relationElement.addProperty("rid", "ink." + nid + "." + nidCounter++);
                            relationElement.addProperty("n1", nid);
                            relationElement.addProperty("name1", currentNodeData.getNameWithAlias());
                            metaData.addProperty("n1", nid);
                            metaData.addProperty("p1", relation.getFromRequirement());
                            JsonArray relationship = new JsonArray();
                            relationship.add(fromComponentNodeName);
                            String requirementRelationshipType = findRequirementType(relation.getFromRequirement(), currentNodeData.getRequirements());
                            if (requirementRelationshipType != null) {
                                relationship.add(requirementRelationshipType);
                            } else {
                                relationship.add((JsonElement) null);
                            }
                            relationship.add(toComponentNodeName);
                            metaData.add(RELATIONSHIP, relationship);
                            findFrom = true;
                        }

                    }

                    String finalToComponentNodeName = toComponentNodeName;
                    String finalToComponentAlias = toComponentAlias;
                    isFoundComposition = templateInfo.getComposition().stream()
                            .filter(element -> finalToComponentAlias.equalsIgnoreCase(element.getAlias())
                                    && element.getAlias().equalsIgnoreCase(currentNodeData.getAliasBelong())
                                    && element.getAlias().equalsIgnoreCase(item.getAlias())
                                    && element.getType().equalsIgnoreCase(item.getItem().getName())
                                    && finalToComponentNodeName.equalsIgnoreCase(currentNodeData.getName())).findAny();
                    if (isFoundComposition.isPresent()) {
                        boolean isFound = findNode(relation.getToCapability(), currentNodeData.getCapabilities());
                        if (isFound) {
                            if (findTo) {
                                report.addErrorMessage("Found 2 match nodes, using the second one. to relation: " + relation.getToCapability());
                            }
                            relationElement.addProperty("n2", nid);
                            relationElement.addProperty("name2",  currentNodeData.getNameWithAlias());
                            metaData.addProperty("n2", nid);
                            metaData.addProperty("p2", relation.getToCapability());
                            findTo = true;
                        }
                    }
                }
            }
            if (findTo && findFrom) {
                relationElement.add("meta", metaData);
                jsonArrayRelations.add(relationElement);
            } else {
                report.addErrorMessage("Didn't find match relation from: " + relation.getFromComponent() + ", to: "+ relation.getToComponent());
            }
        }

        return jsonArrayRelations;
    }

    private String findRequirementType(String fromRequirement, JsonArray requirements) {
        Iterator<JsonElement> jsonElements = requirements.iterator();
        while (jsonElements.hasNext()) {
            JsonObject jsonObject = (JsonObject) jsonElements.next();
            String name = jsonObject.get("name").getAsString();
            if (fromRequirement.equals(name) && jsonObject.has("type")) {
                return jsonObject.get("type").toString().replaceAll("\"", "");
            }
        }

        return null;
    }

    private boolean findNode(String endPoint, JsonArray node) {
        Iterator<JsonElement> jsonElements = node.iterator();
        while (jsonElements.hasNext()) {
            JsonObject jsonObject = (JsonObject) jsonElements.next();
            String name = jsonObject.get("name").getAsString();
            if (endPoint.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private JsonObject newNodeTemplate(String name, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", description);
        return json;
    }

    private JsonObject generateCdumpInput(TemplateInfo templateInfo) {
        JsonObject json = new JsonObject();
        json.addProperty("version", 0);
        json.addProperty("flowType", templateInfo.getFlowType());
        json.add(NODES, new JsonArray());

        json.add("inputs", new JsonArray());
        json.add("outputs", new JsonArray());

        return json;

    }
}

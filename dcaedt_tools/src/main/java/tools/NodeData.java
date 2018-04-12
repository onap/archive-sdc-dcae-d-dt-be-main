package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NodeData {
    private final JsonArray capabilities;
    private final JsonArray requirements;
    private final JsonArray properties;
    private final JsonObject typeInfo;
    private final String nodeName;

    NodeData(JsonArray capabilities, JsonArray requirements, JsonArray properties, JsonObject typeInfo, String nodeName) {
        this.capabilities = capabilities;
        this.requirements = requirements;
        this.properties = properties;
        this.typeInfo = typeInfo;
        this.nodeName = nodeName;
    }

    public JsonArray getCapabilities() {
        return capabilities;
    }

    public JsonArray getRequirements() {
        return requirements;
    }

    public JsonArray getProperties() {
        return properties;
    }

    public JsonObject getTypeInfo() {
        return typeInfo;
    }

    public String getName() {
        return nodeName;
    }
}

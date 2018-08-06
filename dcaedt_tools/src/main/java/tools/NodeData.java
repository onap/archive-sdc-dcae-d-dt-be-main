package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class NodeData {
    private final JsonArray capabilities;
    private final JsonArray requirements;
    private final JsonArray properties;
    private final JsonObject typeInfo;
    private final String nodeName;
    private final String aliasBelong;

    NodeData(JsonArray capabilities, JsonArray requirements, JsonArray properties, JsonObject typeInfo, String nodeName, String aliasBelong) {
        this.capabilities = capabilities;
        this.requirements = requirements;
        this.properties = properties;
        this.typeInfo = typeInfo;
        this.nodeName = nodeName;
        this.aliasBelong = aliasBelong;
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

    public String getNameWithAlias() {
        if (StringUtils.isBlank(aliasBelong)) {
            return nodeName;
        } else {
            return aliasBelong + "." + nodeName;
        }
    }

    public String getAliasBelong() {
        return aliasBelong;
    }
}

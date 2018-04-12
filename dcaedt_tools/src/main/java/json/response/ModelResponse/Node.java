
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Node {

    @SerializedName("capabilities")
    private List<Capability> mCapabilities;
    @SerializedName("description")
    private String mDescription;
    @SerializedName("name")
    private String mName;
    @SerializedName("properties")
    private List<Property> mProperties;
    @SerializedName("requirements")
    private List<Requirement> mRequirements;
    @SerializedName("type")
    private String mType;

    public List<Capability> getCapabilities() {
        return mCapabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        mCapabilities = capabilities;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public List<Property> getProperties() {
        return mProperties;
    }

    public void setProperties(List<Property> properties) {
        mProperties = properties;
    }

    public List<Requirement> getRequirements() {
        return mRequirements;
    }

    public void setRequirements(List<Requirement> requirements) {
        mRequirements = requirements;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

}

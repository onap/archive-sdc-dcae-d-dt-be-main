
package json.templateInfo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Relation {
    @SerializedName("fromComponent")
    private String fromComponent;
    @SerializedName("fromRequirement")
    private String fromRequirement;
    @SerializedName("toComponent")
    private String toComponent;
    @SerializedName("toCapability")
    private String toCapability;

    public String getFromComponent() {
        return fromComponent;
    }

    public void setFromComponent(String fromComponent) {
        this.fromComponent = fromComponent;
    }

    public String getFromRequirement() {
        return fromRequirement;
    }

    public void setFromRequirement(String fromRequirement) {
        this.fromRequirement = fromRequirement;
    }

    public String getToComponent() {
        return toComponent;
    }

    public void setToComponent(String toComponent) {
        this.toComponent = toComponent;
    }

    public String getToCapability() {
        return toCapability;
    }

    public void setToCapability(String toCapability) {
        this.toCapability = toCapability;
    }
}

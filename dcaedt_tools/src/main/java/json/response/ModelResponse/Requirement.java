
package json.response.ModelResponse;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Requirement {

    @SerializedName("capability")
    private Capability mCapability;
    @SerializedName("name")
    private String mName;
    @SerializedName("node_filter")
    private NodeFilter mNodeFilter;
    @SerializedName("relationship")
    private Relationship mRelationship;

    public Capability getCapability() {
        return mCapability;
    }

    public void setCapability(Capability capability) {
        mCapability = capability;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public NodeFilter getNodeFilter() {
        return mNodeFilter;
    }

    public void setNodeFilter(NodeFilter nodeFilter) {
        mNodeFilter = nodeFilter;
    }

    public Relationship getRelationship() {
        return mRelationship;
    }

    public void setRelationship(Relationship relationship) {
        mRelationship = relationship;
    }

}

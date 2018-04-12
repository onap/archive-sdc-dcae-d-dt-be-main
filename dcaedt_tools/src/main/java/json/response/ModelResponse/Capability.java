
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Capability {

    @SerializedName("dcae.capabilities.stream.subscribe")
    private DcaeCapabilitiesStreamSubscribe mDcaeCapabilitiesStreamSubscribe;
    @SerializedName("id")
    private String mId;
    @SerializedName("name")
    private String mName;
    @SerializedName("properties")
    private List<Property> mProperties;
    @SerializedName("type")
    private Type mType;

    public DcaeCapabilitiesStreamSubscribe getDcaeCapabilitiesStreamSubscribe() {
        return mDcaeCapabilitiesStreamSubscribe;
    }

    public void setDcaeCapabilitiesStreamSubscribe(DcaeCapabilitiesStreamSubscribe dcaeCapabilitiesStreamSubscribe) {
        mDcaeCapabilitiesStreamSubscribe = dcaeCapabilitiesStreamSubscribe;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
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

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

}

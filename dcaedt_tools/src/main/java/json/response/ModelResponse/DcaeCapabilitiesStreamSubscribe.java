
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class DcaeCapabilitiesStreamSubscribe {

    @SerializedName("properties")
    private List<Property> mProperties;

    public List<Property> getProperties() {
        return mProperties;
    }

    public void setProperties(List<Property> properties) {
        mProperties = properties;
    }

}

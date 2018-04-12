
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class NodeFilter {

    @SerializedName("capabilities")
    private List<Capability> mCapabilities;

    public List<Capability> getCapabilities() {
        return mCapabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        mCapabilities = capabilities;
    }

}

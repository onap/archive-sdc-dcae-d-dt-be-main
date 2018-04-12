
package json.response.ModelResponse;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Data {

    @SerializedName("model")
    private Model mModel;

    public Model getModel() {
        return mModel;
    }

    public void setModel(Model model) {
        mModel = model;
    }

}

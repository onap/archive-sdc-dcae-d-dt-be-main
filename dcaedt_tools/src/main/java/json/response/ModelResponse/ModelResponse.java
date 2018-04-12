
package json.response.ModelResponse;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class ModelResponse {

    @SerializedName("data")
    private Data mData;
    @SerializedName("error")
    private Error mError;
    @SerializedName("id")
    private Object mId;
    @SerializedName("timestamp")
    private Long mTimestamp;

    public Data getData() {
        return mData;
    }

    public void setData(Data data) {
        mData = data;
    }

    public Error getError() {
        return mError;
    }

    public void setError(Error error) {
        mError = error;
    }

    public Object getId() {
        return mId;
    }

    public void setId(Object id) {
        mId = id;
    }

    public Long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(Long timestamp) {
        mTimestamp = timestamp;
    }

}

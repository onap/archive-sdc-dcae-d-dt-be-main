
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Property {

    @SerializedName("assignment")
    private Assignment mAssignment;
    @SerializedName("format")
    private List<Format> mFormat;
    @SerializedName("name")
    private String mName;
    @SerializedName("type")
    private String mType;

    public Assignment getAssignment() {
        return mAssignment;
    }

    public void setAssignment(Assignment assignment) {
        mAssignment = assignment;
    }

    public List<Format> getFormat() {
        return mFormat;
    }

    public void setFormat(List<Format> format) {
        mFormat = format;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

}

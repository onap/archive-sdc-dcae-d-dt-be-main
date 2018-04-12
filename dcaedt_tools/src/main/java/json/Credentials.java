
package json;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Credentials {

    @SerializedName("credentials")
    private List<Credential> Credentials;

    public List<Credential> getCredentials() {
        return Credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        Credentials = credentials;
    }

}

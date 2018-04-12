
package json;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Environment {

    @SerializedName("apiPath")
    private String ApiPath;
    @SerializedName("dcaeBeHost")
    private String DcaeBeHost;
    @SerializedName("dcaeBePort")
    private String DcaeBePort;
    @SerializedName("credential")
    private Credential credential;

    public String getApiPath() {
        return ApiPath;
    }

    public void setApiPath(String apiPath) {
        ApiPath = apiPath;
    }

    public String getDcaeBeHost() {
        return DcaeBeHost;
    }

    public void setDcaeBeHost(String dcaeBeHost) {
        DcaeBeHost = dcaeBeHost;
    }

    public String getDcaeBePort() {
        return DcaeBePort;
    }

    public void setDcaeBePort(String dcaeBePort) {
        DcaeBePort = dcaeBePort;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

}

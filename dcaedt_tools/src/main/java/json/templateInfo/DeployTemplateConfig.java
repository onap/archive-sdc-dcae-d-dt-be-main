package json.templateInfo;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class DeployTemplateConfig {

    @SerializedName("templateInfo")
    private List<TemplateInfo> TemplateInfo;

    public List<TemplateInfo> getTemplateInfo() {
        return TemplateInfo;
    }

    public void setTemplateInfo(List<TemplateInfo> templateInfo) {
        TemplateInfo = templateInfo;
    }

}

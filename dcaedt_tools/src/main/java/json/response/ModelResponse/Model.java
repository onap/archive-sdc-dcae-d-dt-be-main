
package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Model {

    @SerializedName("catalog")
    private String mCatalog;
    @SerializedName("catalogId")
    private Long mCatalogId;
    @SerializedName("id")
    private Long mId;
    @SerializedName("itemId")
    private String mItemId;
    @SerializedName("name")
    private String mName;
    @SerializedName("nodes")
    private List<Node> mNodes;

    public String getCatalog() {
        return mCatalog;
    }

    public void setCatalog(String catalog) {
        mCatalog = catalog;
    }

    public Long getCatalogId() {
        return mCatalogId;
    }

    public void setCatalogId(Long catalogId) {
        mCatalogId = catalogId;
    }

    public Long getId() {
        return mId;
    }

    public void setId(Long id) {
        mId = id;
    }

    public String getItemId() {
        return mItemId;
    }

    public void setItemId(String itemId) {
        mItemId = itemId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public List<Node> getNodes() {
        return mNodes;
    }

    public void setNodes(List<Node> nodes) {
        mNodes = nodes;
    }

}

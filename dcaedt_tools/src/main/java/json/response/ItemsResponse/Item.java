
package json.response.ItemsResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Item {

    @SerializedName("artifacts")
    private List<Artifact> mArtifacts;
    @SerializedName("catalog")
    private String mCatalog;
    @SerializedName("catalogId")
    private Long mCatalogId;
    @SerializedName("category")
    private String mCategory;
    @SerializedName("description")
    private String mDescription;
    @SerializedName("id")
    private Long mId;
    @SerializedName("invariantUUID")
    private String mInvariantUUID;
    @SerializedName("itemId")
    private String mItemId;
    @SerializedName("lastUpdaterFullName")
    private String mLastUpdaterFullName;
    @SerializedName("lastUpdaterUserId")
    private String mLastUpdaterUserId;
    @SerializedName("lifecycleState")
    private String mLifecycleState;
    @SerializedName("models")
    private List<Model> mModels;
    @SerializedName("name")
    private String mName;
    @SerializedName("resourceType")
    private String mResourceType;
    @SerializedName("subCategory")
    private String mSubCategory;
    @SerializedName("toscaModelURL")
    private String mToscaModelURL;
    @SerializedName("toscaResourceName")
    private String mToscaResourceName;
    @SerializedName("uuid")
    private String mUuid;
    @SerializedName("version")
    private String mVersion;

    public List<Artifact> getArtifacts() {
        return mArtifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        mArtifacts = artifacts;
    }

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

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public Long getId() {
        return mId;
    }

    public void setId(Long id) {
        mId = id;
    }

    public String getInvariantUUID() {
        return mInvariantUUID;
    }

    public void setInvariantUUID(String invariantUUID) {
        mInvariantUUID = invariantUUID;
    }

    public String getItemId() {
        return mItemId;
    }

    public void setItemId(String itemId) {
        mItemId = itemId;
    }

    public String getLastUpdaterFullName() {
        return mLastUpdaterFullName;
    }

    public void setLastUpdaterFullName(String lastUpdaterFullName) {
        mLastUpdaterFullName = lastUpdaterFullName;
    }

    public String getLastUpdaterUserId() {
        return mLastUpdaterUserId;
    }

    public void setLastUpdaterUserId(String lastUpdaterUserId) {
        mLastUpdaterUserId = lastUpdaterUserId;
    }

    public String getLifecycleState() {
        return mLifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        mLifecycleState = lifecycleState;
    }

    public List<Model> getModels() {
        return mModels;
    }

    public void setModels(List<Model> models) {
        mModels = models;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getResourceType() {
        return mResourceType;
    }

    public void setResourceType(String resourceType) {
        mResourceType = resourceType;
    }

    public String getSubCategory() {
        return mSubCategory;
    }

    public void setSubCategory(String subCategory) {
        mSubCategory = subCategory;
    }

    public String getToscaModelURL() {
        return mToscaModelURL;
    }

    public void setToscaModelURL(String toscaModelURL) {
        mToscaModelURL = toscaModelURL;
    }

    public String getToscaResourceName() {
        return mToscaResourceName;
    }

    public void setToscaResourceName(String toscaResourceName) {
        mToscaResourceName = toscaResourceName;
    }

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

}


package json.templateInfo;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class TemplateInfo {

    @SerializedName("category")
    private String Category;
    @SerializedName("composition")
    private List<json.templateInfo.Composition> Composition;
    @SerializedName("description")
    private String Description;
    @SerializedName("name")
    private String Name;
    @SerializedName("relations")
    private List<Relation> Relations;
    @SerializedName("subCategory")
    private String SubCategory;
    @SerializedName("updateIfExist")
    private Boolean UpdateIfExist;

    public String getCategory() {
        return Category;
    }

    public void setCategory(String category) {
        Category = category;
    }

    public List<json.templateInfo.Composition> getComposition() {
        return Composition;
    }

    public void setComposition(List<json.templateInfo.Composition> composition) {
        Composition = composition;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public List<Relation> getRelations() {
        return Relations;
    }

    public void setRelations(List<Relation> relations) {
        Relations = relations;
    }

    public String getSubCategory() {
        return SubCategory;
    }

    public void setSubCategory(String subCategory) {
        SubCategory = subCategory;
    }

    public Boolean getUpdateIfExist() {
        return UpdateIfExist;
    }

    public void setUpdateIfExist(Boolean updateIfExist) {
        UpdateIfExist = updateIfExist;
    }

}

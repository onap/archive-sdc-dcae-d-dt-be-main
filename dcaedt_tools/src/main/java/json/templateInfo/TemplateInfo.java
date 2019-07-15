/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

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
    @SerializedName("flowType")
    private String FlowType;
    @SerializedName("relations")
    private List<Relation> Relations;
    @SerializedName("nodesToDelete")
    private List<NodeToDelete> NodesToDelete;
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

    public String getFlowType() {
        return FlowType;
    }

    public void setFlowType(String flowType) {
        FlowType = flowType;
    }

    public List<NodeToDelete> getNodesToDelete() {
        return NodesToDelete;
    }

    public void setNodesToDelete(List<NodeToDelete> nodesToDelete) {
        NodesToDelete = nodesToDelete;
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

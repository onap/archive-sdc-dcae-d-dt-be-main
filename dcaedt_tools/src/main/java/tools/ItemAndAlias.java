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

package tools;

import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;

public class ItemAndAlias {
    private final Resource item;
    private final String alias;
    public ItemAndAlias(Resource item, String alias) {
        this.item = item;
        this.alias = alias;
    }

    public Resource getItem() {
        return item;
    }

    public String getAlias() {
        return alias;
    }
}

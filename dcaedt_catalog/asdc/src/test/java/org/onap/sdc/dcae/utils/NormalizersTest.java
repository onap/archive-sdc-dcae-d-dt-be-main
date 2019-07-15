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

package org.onap.sdc.dcae.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NormalizersTest {

    @Test
    public void normalizeVFCMTName_withDot_withoutDot(){
        Assertions.assertThat(Normalizers.normalizeComponentName("my.dot")).isEqualTo("MyDot");
    }

    @Test
    public void normalizeVFCMTName_withUnderscore_withoutUnderscore(){
        Assertions.assertThat(Normalizers.normalizeComponentName("My_Monitoring_Template_example")).isEqualTo("MyMonitoringTemplateExample");
    }

    @Test
    public void normalizeVFCMTName_withWhiteSpace_withoutWhiteSpace(){
        Assertions.assertThat(Normalizers.normalizeComponentName("       my        dot     ")).isEqualTo("MyDot");
    }

    @Test
    public void normalizeVFCMTName_withDash_withoutDash(){
        Assertions.assertThat(Normalizers.normalizeComponentName("My-Monitoring-Template-example")).isEqualTo("MyMonitoringTemplateExample");
    }

    @Test
    public void normalizeVFCMTName_notCapitalized_capitalized(){
        Assertions.assertThat(Normalizers.normalizeComponentName("my monitoring template eXAMPLE")).isEqualTo("MyMonitoringTemplateExample");
    }

    @Test
    public void normalizeArtifactLabel_withDash_withoutDash(){
        Assertions.assertThat(Normalizers.normalizeArtifactLabel("blueprint-other")).isEqualTo("blueprintother");
    }

    @Test
    public void normalizeArtifactLabel_withWhiteSpace_withoutWhiteSpace(){
        Assertions.assertThat(Normalizers.normalizeArtifactLabel("       blueprint        other")).isEqualTo("blueprintother");
    }

    @Test
    public void normalizeArtifactLabel_withPlus_withoutPlus(){
        Assertions.assertThat(Normalizers.normalizeArtifactLabel("+blueprint+++other+")).isEqualTo("blueprintother");
    }
}

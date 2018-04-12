package org.onap.sdc.dcae.utils;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.onap.sdc.dcae.utils.Normalizers;


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

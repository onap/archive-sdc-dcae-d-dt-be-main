package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.dcae.checker.Checker;
import org.onap.sdc.dcae.checker.Construct;

import java.util.Map;

public class ArtifactCommon extends BaseCommon {

    private static ArtifactCommon instance;

    public synchronized static ArtifactCommon getInstance() {
        if (instance == null)
        {
            instance = new ArtifactCommon();
        }
        return instance;
    }

    private ArtifactCommon() {}
    public void checkArtifactTypeDefinition(String theName,
                                             Map theDefinition,
                                             Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Artifact);
        try {
            CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext);
        } finally {
            theContext.exit();
        }
    }
}

package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;

public class BaseCommon {
    protected static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    protected static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
}

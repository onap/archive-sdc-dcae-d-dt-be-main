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

import org.apache.commons.lang3.text.WordUtils;

import java.util.regex.Pattern;

public final class Normalizers {

    private static final Pattern COMPONENT_NAME_DELIMITER_PATTERN = Pattern.compile("[.\\-_]+");
    private static final Pattern ARTIFACT_LABEL_DELIMITER_PATTERN = Pattern.compile("[ \\-+._]+");
    private static final Pattern COMPONENT_INSTANCE_NAME_DELIMITER_PATTERN = Pattern.compile("[ \\-.]+");


    public static String normalizeComponentName(String name) {
        String normalizedName = name.toLowerCase();
        normalizedName = COMPONENT_NAME_DELIMITER_PATTERN.matcher(normalizedName).replaceAll(" ");
        String[] split = normalizedName.split(" ");
        StringBuffer sb = new StringBuffer();
        for (String splitElement : split) {
            String capitalize = WordUtils.capitalize(splitElement);
            sb.append(capitalize);
        }
        return sb.toString();
    }

    public static String normalizeArtifactLabel(String label) {
        return ARTIFACT_LABEL_DELIMITER_PATTERN.matcher(label).replaceAll("").toLowerCase();
    }

    public static String normalizeComponentInstanceName(String name) {
        return COMPONENT_INSTANCE_NAME_DELIMITER_PATTERN.matcher(name).replaceAll("").toLowerCase();
    }

}

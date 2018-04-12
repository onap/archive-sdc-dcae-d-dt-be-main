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

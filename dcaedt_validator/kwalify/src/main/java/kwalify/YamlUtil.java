/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 *  utilify class for yaml.
 */
public class YamlUtil {

    private YamlUtil() {
        //hides implicit public
    }

    public static Object load(String yamlStr) throws SyntaxException {
        PlainYamlParser parser = new PlainYamlParser(yamlStr);
        return parser.parse();
    }
}

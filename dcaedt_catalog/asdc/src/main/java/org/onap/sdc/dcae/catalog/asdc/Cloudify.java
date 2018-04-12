package org.onap.sdc.dcae.catalog.asdc;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.commons.ListBuilder;
import org.onap.sdc.dcae.catalog.commons.MapBuilder;
import org.onap.sdc.dcae.checker.Catalog;
import org.onap.sdc.dcae.checker.Construct;
import org.onap.sdc.dcae.checker.Target;

import com.google.common.collect.Lists;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


public class Cloudify {

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	Catalog catalog;

	public Cloudify(Catalog c)
	{
		catalog = c;
	}
	public class ModelTemplate {
		public Map<String, Map> template;
		public JXPathContext jx;
		public String node;
		public ModelTemplate(Map<String, Map> t, JXPathContext j, String node_name)
		{
			template = t;
			jx = j;
			node = node_name;
		}		
		
		public Object getPropValue(JXPathContext jx_src, String name)
		{
			try{
				Object ret = jx_src.getValue("properties/"+name+"/get_input");
				if (ret==null)
					return jx_src.getValue("properties/"+name);
				return getDefaultPropValue((String)ret);
			}
			catch (RuntimeException e) {

			}
			try{
				return jx_src.getValue("properties/"+name+"");
			}
			catch (RuntimeException e) {
				return null;
			}						
		}
		
		public Object getDefaultPropValue(String name) {
			try {
				return jx.getValue("//"+name+"/default");
			}
			catch (RuntimeException e) {
				return null;
			}

		}
	}
	
	public class ModelTranslate {
		public Map<String, Map> template;
		public JXPathContext jx;
		public String node;

		public ModelTranslate(Map<String, Map> t, JXPathContext j, String node_name)
		{
			template = t;
			jx = j;
			node = node_name;
		}
		
		public String getTranslateName()
		{
			Map<String, Object> node_temp = (Map<String, Object>)jx.getValue("//node_templates");
			Iterator it = node_temp.keySet().iterator();
			if (it.hasNext())
				return node + "_"+ it.next();
			else
				return null;
		}
		
		public Map<String, Object> translate(JXPathContext jx_src, Map<String, Map> model_lib, String node_name)
		{
			for (Iterator prop_iter = jx.iteratePointers("//*[@get_input]"); prop_iter.hasNext();) {

				Pointer p = (Pointer)prop_iter.next();
				JXPathContext prop_path = jx.getRelativeContext(p);

				ModelTemplate src_model =(ModelTemplate) model_lib.get(node_name).get("model");

				Object temp_o = src_model.getPropValue(jx_src, (String) prop_path.getValue("get_input"));
				//prop_path.setValue(".", temp_o);
				jx.setValue(p.asPath(), temp_o);
			}
			
//			JXPathContext jx_src = JXPathContext.newContext(src);
			for (Iterator req_iter = jx_src.iteratePointers("//*/node"); req_iter.hasNext();) {
				Pointer p = (Pointer)req_iter.next();
				String req_node_name = (String)jx_src.getValue(p.asPath());

				for (Iterator it = model_lib.keySet().iterator(); it.hasNext();) {
					String key = (String) it.next();
					if (key.indexOf(req_node_name) <0 )
						continue;
					ModelTranslate tt = (ModelTranslate) model_lib.get(key).get("translate");
					if (tt == null)
						req_node_name = null;
					else
					{
						req_node_name = tt.getTranslateName();
					}
					break;					
				}	
				
			}
			
			String tn_name = getTranslateName();
			
			if (tn_name == null)		
				return (Map<String, Object>)jx.getValue("//node_templates");
			else
				return (new MapBuilder<String, Object>().put(tn_name, jx.getValue("//node_templates/*")).build());
		}
		
	}
	
	public ModelTranslate findTranslateTemplate(String ty, String node) {
		for (Target t: catalog.targets()) {

			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTranslateTemplate: target {}", t.getName());
			if (t.getName().startsWith("translat") == false) {
				continue;
			}

			Map<String, Map>temp = (Map<String, Map>)t.getTarget();
			
			JXPathContext jxroot = JXPathContext.newContext(temp);
			try{
				String sub_type = (String)jxroot.getValue("topology_template/substitution_mappings/node_type");
				if (sub_type != null && sub_type.equals(ty)) {
					return new ModelTranslate(temp, jxroot, node);
				}
			}
			catch (RuntimeException e) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "translate template {} does not have substitution mapping section", t.getName());
			}
		}
		return null;
	}
	
	public ModelTemplate findModelTemplate(String ty, String node) {
		for (Target t: catalog.targets()) {

			if (t.getName().startsWith("templat") == false)
				continue;
			Map<String, Map>temp = (Map<String, Map>)t.getTarget();
			
			JXPathContext jxroot = JXPathContext.newContext(temp);
			for (Iterator it = jxroot.iterate("topology_template/node_templates/*/type"); it.hasNext();) {
				String node_type = (String)it.next();
				if (node_type != null && node_type.equals(ty)) {
					return new ModelTemplate(temp, jxroot, node);
				}
			}
		}
		return null;
	}
	
	public Map<String, Object> createBlueprint()	{
		
		Map<String, Map> target_temp = null; 
		for (Target t: catalog.targets()) {

			if (t.getName().equals("cdump")) {
				target_temp = catalog.getTargetTemplates(t, Construct.Node);
			}
		}

		JXPathContext jxroot = JXPathContext.newContext(target_temp);
		
		Map<String, Object> output_temp = new HashMap<String, Object>();
		Map<String, Map> model_lib = new HashMap<String, Map>();

		for (Iterator iter = target_temp.keySet().iterator(); iter.hasNext();)
		{
			String node_key = (String)iter.next();
			//jxroot.getVariables().declareVariable("name", target_temp.get(node_key));
			//String node_type = (String)jxroot.getValue("$name/type");
			String node_type = (String)jxroot.getValue(node_key+"/type");

			ModelTranslate t_temp = findTranslateTemplate(node_type, node_key);
			ModelTemplate t_model = findModelTemplate(node_type, node_key);
			
			model_lib.put(node_key, new MapBuilder()
																			.put("model", t_model)
																			.put("translate", t_temp)
																			.build());
		}
		
		for (Iterator iter = model_lib.keySet().iterator(); iter.hasNext();) {
			String node_key = (String) iter.next();
			ModelTranslate t =  (ModelTranslate) model_lib.get(node_key).get("translate");
			JXPathContext jxnode = jxroot.getRelativeContext(jxroot.getPointer(node_key));
			if (t != null) {
				Map<String, Object> t_output =t.translate(jxnode, model_lib, node_key);
				if (t_output != null)
					output_temp.putAll(t_output);
			}

		}
		
		return new MapBuilder<String, Object>()
											.put("tosca_definitions_version", new String("cloudify_dsl_1_3"))
											.put("imports", new ListBuilder()
																				.add(new MapBuilder()
																								.put("cloudify",
																										 "http://www.getcloudify.org/spec/cloudify/3.4/types.yaml")
																								.build())
																				.build())
											.put("node_templates", output_temp)
											.build();

	}

	public String createBlueprintDocument() {
		DumperOptions options = new DumperOptions();
		options.setWidth(1000000);
		Yaml yaml = new Yaml(options);
		return yaml.dump(createBlueprint());
	}
}

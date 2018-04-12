package org.onap.sdc.dcae.catalog.commons;

import java.util.Iterator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import org.json.JSONObject;


public class Neo {

  /*
   */
  public static String literalMap(JSONObject theProps,
                                  String theNameAlias,
                                  String theValueAlias,
                                  String theAssignmentOp,
																	String theRelationOp,
                                  Predicate theFieldFilter) {
    if(theProps.length() == 0)
      return "";
    StringBuilder sb = new StringBuilder("");
    for (Iterator i = Iterators.filter(theProps.keys(),
                                       theFieldFilter);
         i.hasNext();) {
      String propName = (String)i.next();

      if (theNameAlias != null) {
        sb.append(theNameAlias)
          .append('.');
      }
      sb.append('`')
        .append(propName)
        .append('`')
        .append(theAssignmentOp)
        .append(" {")
        .append(theValueAlias)
        .append("}.")
        .append('`')
        .append(propName)
        .append('`')
        .append(theRelationOp);
    }
    return sb.substring(0, sb.length() - theRelationOp.length());
  }

  public static String literalMap(JSONObject theProps,
                                  String theAlias) {
    return literalMap(theProps, null, theAlias, ":", ",", f -> true);
  }

}


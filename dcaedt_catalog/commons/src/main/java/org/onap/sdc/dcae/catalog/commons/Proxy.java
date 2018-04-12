package org.onap.sdc.dcae.catalog.commons;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Collections;

import java.util.stream.Collectors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;

import java.lang.invoke.MethodHandles;

import com.google.common.reflect.Invokable;
import org.onap.sdc.dcae.catalog.commons.Proxy;
import org.onap.sdc.dcae.catalog.commons.ProxyBuilder;
import com.google.common.reflect.AbstractInvocationHandler;

import org.apache.commons.beanutils.ConvertUtils;

import org.json.JSONObject;
import org.json.JSONArray;

public class Proxy extends AbstractInvocationHandler {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)

    public static @interface DataMap {

        public String map() default "";

        public boolean proxy() default false;

        public Class elementType() default Void.class;
    }


    public static final Constructor<MethodHandles.Lookup> lookupHandleConstructor;

    static {
        try {
            lookupHandleConstructor =
                    MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,
                                                                                                                        int.class);

            if (!lookupHandleConstructor.isAccessible()) {
            lookupHandleConstructor.setAccessible(true);
            }
        }
        catch (Exception x) {
            throw new RuntimeException(x);
        }
    }


    private JSONObject		data;
    private ProxyBuilder	builder;

    protected Proxy(JSONObject theData, ProxyBuilder theBuilder) {
        this.data = theData;
        this.builder = theBuilder;
    }

    public JSONObject data() {
        return this.data;
    }

    public ProxyBuilder getBuilder() {
        return this.builder;
    }

    protected Object handleInvocation(
                                            Object theProxy,Method theMethod,Object[] theArgs)
                                                                                                                throws Throwable {
        if (theMethod.isDefault()) {
                final Class<?> declaringClass = theMethod.getDeclaringClass();

            return lookupHandleConstructor
                            .newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                            .unreflectSpecial(theMethod, declaringClass)
                            .bindTo(theProxy)
                            .invokeWithArguments(theArgs);
        }

        String key = theMethod.getName();

        Proxy.DataMap dataMap = (Proxy.DataMap)theMethod.getAnnotation(Proxy.DataMap.class);
        if (dataMap != null) {
            String dataKey = dataMap.map();
            if (dataKey != null && !"".equals(dataKey))
                key = dataKey;
        }

        //this is ugly, can this be done through an extension mechanism such as plugging in functions?
        if ( builder.hasExtension(key) )
            return this.builder.extension(key).apply(this, theArgs);

        //we give priority to the context (because of the 'catalog' property issue in catalog service) but
        //how natural is this?
        Object val = this.builder.context(key);
        if (val == null)
            val = this.data.opt(key);

        if (val == null)
            return null;

//as we create proxies here we should store them back in the 'data' so that we do not do it again
//can we always 'recognize' them?
        if (val instanceof String &&
                String.class != theMethod.getReturnType()) {
            //??This will yield a POJO ..
            return ConvertUtils.convert((String)val, theMethod.getReturnType());
        }
        else if (val instanceof JSONObject) {
            if (dataMap != null && dataMap.proxy()) {
                return builder.build((JSONObject)val, theMethod.getReturnType());
            }
        }
        else if (val instanceof JSONArray&& dataMap != null &&
                 dataMap.proxy() &&
                 List.class.isAssignableFrom(theMethod.getReturnType())) {

            List res = (List) theMethod.getReturnType().newInstance();
            for (int i = 0; i < ((JSONArray) val).length(); i++) {
                res.add(builder.build(((JSONArray) val).getJSONObject(i), dataMap.elementType()));
            }
            return res;

        }
        return val;
    }
}

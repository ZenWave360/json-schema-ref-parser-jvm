package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.JsonLocation;
import com.jayway.jsonpath.DocumentContext;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface ExtendedJsonContext extends DocumentContext {

    Map<String, Pair<JsonLocation, JsonLocation>> getJsonLocationsMap();

    /**
     * Creates a dynamic proxy for the given {@link com.jayway.jsonpath.DocumentContext} adding getJsonLocationsContext method.
     *
     * @param jsonContext
     * @param locations
     * @return
     */
    static ExtendedJsonContext of(DocumentContext jsonContext, Map<String, Pair<JsonLocation, JsonLocation>> locations) {
        BiFunction<Class, Method, Method> findMethod = (Class clazz, Method m) -> {
            try {
                return clazz.getMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return null;
            }
        };

        Supplier<Map<String, Pair<JsonLocation, JsonLocation>>> getJsonLocationsMap = () -> {
            return locations;
        };

        return (ExtendedJsonContext) Proxy.newProxyInstance(
                jsonContext.getClass().getClassLoader(),
                new Class[] { ExtendedJsonContext.class },
                // this is the InvocationHandler implementation
                (proxy, method, args)  -> {
                    if("toString".equals(method.getName())) {
                        // using jsonContext reference, so it appears in the debugger as lambda var
                        return "ExtendedJsonContext@" + jsonContext.hashCode() + locations.hashCode();
                    }
//                    System.out.println("ExtendedJsonContext invoking method " + method.getName());

                    var m = findMethod.apply(DocumentContext.class, method);
                    if (m != null) {
//                        System.out.println("ExtendedJsonContext invoking DocumentContext class for " + method.getName());
                        return m.invoke(jsonContext, args);
                    }
                    m = findMethod.apply(ExtendedJsonContext.class, method);
                    if (m != null) {
//                        System.out.println("ExtendedJsonContext invoking getJsonLocationsMap class for " + method.getName());
                        return getJsonLocationsMap.get();
                    }
//                    System.out.println("ExtendedJsonContext invoking super class for " + method.getName());
                    return method.invoke(proxy, args);
                });
    }
}

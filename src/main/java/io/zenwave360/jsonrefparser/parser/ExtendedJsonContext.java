package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ExtendedJsonContext extends DocumentContext {

    JsonLocationsContext getJsonLocationsContext();

    /**
     * Creates a dynamic proxy for the given {@link com.jayway.jsonpath.DocumentContext} adding getJsonLocationsContext method.
     *
     * @param jsonContext
     * @param jsonLocationsContext
     * @return
     */
    static ExtendedJsonContext of(DocumentContext jsonContext, JsonLocationsContext jsonLocationsContext) {
        BiFunction<Class, Method, Method> findMethod = (Class clazz, Method m) -> {
            try {
                return clazz.getMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return null;
            }
        };

        Supplier<JsonLocationsContext> getJsonLocationsContext = () -> {
            return jsonLocationsContext;
        };

        return (ExtendedJsonContext) Proxy.newProxyInstance(
                jsonContext.getClass().getClassLoader(),
                new Class[] { ExtendedJsonContext.class },
                // this is the InvocationHandler implementation
                (proxy, method, args)  -> {
                    if("toString".equals(method.getName())) {
                        // using jsonContext reference, so it appears in the debugger as lambda var
                        return "ExtendedJsonContext@" + jsonContext.hashCode() + jsonLocationsContext.hashCode();
                    }
//                    System.out.println("ExtendedJsonContext invoking method " + method.getName());

                    var m = findMethod.apply(DocumentContext.class, method);
                    if (m != null) {
//                        System.out.println("ExtendedJsonContext invoking DocumentContext class for " + method.getName());
                        return m.invoke(jsonContext, args);
                    }
                    m = findMethod.apply(ExtendedJsonContext.class, method);
                    if (m != null) {
//                        System.out.println("ExtendedJsonContext invoking getJsonLocationsContext class for " + method.getName());
                        return getJsonLocationsContext.get();
                    }
//                    System.out.println("ExtendedJsonContext invoking super class for " + method.getName());
                    return method.invoke(proxy, args);
                });
    }
}

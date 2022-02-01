package io.zenwave360.jsonrefparser.resolver;

import io.zenwave360.jsonrefparser.$Ref;

public class HttpResolver implements Resolver {
    @Override
    public String resolve($Ref $ref) {
        return "resolved $ref " + $ref;
    }
}

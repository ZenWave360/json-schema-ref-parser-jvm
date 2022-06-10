package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonLocationsContext {

    private Map<String, Pair<JsonLocation, JsonLocation>> locations = new HashMap<>();

    public Map<String, Pair<JsonLocation, JsonLocation>> getLocations() {
        return locations;
    }
    String processDeserializationStart(JsonParser jsonParser, DeserializationContext context) throws IOException {
        var currentTokenId = jsonParser.currentTokenId();
        String pathName = getFullPathName(jsonParser);
        locations.put(pathName, Pair.of(jsonParser.getCurrentLocation(), null));
//        System.out.println(currentTokenId + " " + jsonParser.getCurrentLocation() + " " + pathName);
        return pathName;
    }

    void processDeserializationEnd(String pathName, JsonParser jsonParser, DeserializationContext context) throws IOException {
        Pair<JsonLocation, JsonLocation> location = locations.get(pathName);
        locations.put(pathName, Pair.of(location.getLeft(), jsonParser.getCurrentLocation()));
//        System.out.println(0 + " " + jsonParser.getCurrentLocation() + " " + pathName);
    }

    String getFullPathName(JsonParser jsonParser) throws IOException {
        List<String> paths = new ArrayList<>();
        var context = jsonParser.getParsingContext();
        if(context.inRoot()) {
            paths.add("$");
        } else {
            do {
                if (context.inRoot()) {
                    paths.add("$");
                } else {
                    if(context.inArray() && jsonParser.getCurrentToken() != JsonToken.START_ARRAY) {
                        paths.add(context.getCurrentIndex() + "");
                    } else if (context.getCurrentName() != null) {
                        paths.add(context.getCurrentName());
                    } else {
                        System.out.println("w");
                    }
                }
            } while ((context = context.getParent()) != null);
        }
        Collections.reverse(paths);
        return StringUtils.join(paths, ".");
    }
}

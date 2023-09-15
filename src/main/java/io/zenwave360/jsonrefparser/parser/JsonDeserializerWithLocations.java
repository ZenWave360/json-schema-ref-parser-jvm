package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

import static com.fasterxml.jackson.core.JsonTokenId.ID_START_ARRAY;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_OBJECT;

public class JsonDeserializerWithLocations extends UntypedObjectDeserializer {

    private Map<String, Pair<JsonLocation, JsonLocation>> locations = new HashMap<>();

    public Map<String, Pair<JsonLocation, JsonLocation>> getLocations() {
        return locations;
    }

    JsonDeserializerWithLocations() {
        super(null, null);
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        String pathName = getFullPathName(jsonParser);
        JsonLocation startLocation = ((CustomYAMLParser) jsonParser).getLastEventStartLocation();
        boolean isObjectStart = jsonParser.currentTokenId() == ID_START_OBJECT || jsonParser.currentTokenId() == ID_START_ARRAY;

        if (pathName != null && isObjectStart) {
            saveLocationStart(getFullPathName(jsonParser), startLocation);
        }
//        System.out.println("A: " + jsonParser.currentTokenId()+ " " + startLocation + " " + jsonParser.getCurrentLocation() + " " + pathName);
        Object result = super.deserialize(jsonParser, ctxt);
//        System.out.println("D: " + jsonParser.currentTokenId()+ " " + startLocation + " " + jsonParser.getCurrentLocation() + " " + pathName);
        if (pathName != null && isObjectStart) {
            saveLocationEnd(pathName, jsonParser.getCurrentLocation());
        }
        return result;
    }

    void saveLocationStart(String pathName, JsonLocation start) throws IOException {
        locations.put(pathName, Pair.of(start, null));
    }

    void saveLocationEnd(String pathName, JsonLocation end) throws IOException {
        Pair<JsonLocation, JsonLocation> location = locations.get(pathName);
        locations.put(pathName, Pair.of(location.getLeft(), end));
        //        System.out.println(0 + " " + jsonParser.getCurrentLocation() + " " + pathName);
    }

    String getFullPathName(JsonParser jsonParser) throws IOException {
        List<String> paths = new ArrayList<>();
        com.fasterxml.jackson.core.JsonStreamContext context = jsonParser.getParsingContext();
        if (context.inRoot()) {
            paths.add("$");
        } else {
            do {
                if (context.inRoot()) {
                    paths.add("$");
                } else {
                    if (context.inArray() && jsonParser.getCurrentToken() != JsonToken.START_ARRAY) {
                        paths.add(context.getCurrentIndex() + "");
                    } else if (context.getCurrentName() != null) {
                        paths.add(context.getCurrentName());
                    } else {
//                        System.out.println("w");
                    }
                }
            } while ((context = context.getParent()) != null);
        }
        Collections.reverse(paths);
        return StringUtils.join(paths, ".");
    }
}

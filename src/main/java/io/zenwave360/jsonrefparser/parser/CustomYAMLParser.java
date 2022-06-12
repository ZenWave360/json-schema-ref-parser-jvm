package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.yaml.snakeyaml.events.Event;

import java.io.Reader;

public class CustomYAMLParser extends YAMLParser {

    public CustomYAMLParser(IOContext ctxt, BufferRecycler br, int parserFeatures, int formatFeatures, ObjectCodec codec, Reader reader) {
        super(ctxt, br, parserFeatures, formatFeatures, codec, reader);
    }

    public Event getLastEvent() {
        return _lastEvent;
    }

    public JsonLocation getLastEventStartLocation() {
        // can assume we are at the end of token now...
        if (_lastEvent == null) {
            return JsonLocation.NA;
        }
        return _locationFor(_lastEvent.getStartMark());
    }
}

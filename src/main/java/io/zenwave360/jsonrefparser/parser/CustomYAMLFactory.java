package io.zenwave360.jsonrefparser.parser;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class CustomYAMLFactory extends YAMLFactory {

    @Override
    protected YAMLParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new CustomYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, _createReader(in, null, ctxt));
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new CustomYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, r);
    }

    // since 2.4
    @Override
    protected YAMLParser _createParser(char[] data, int offset, int len, IOContext ctxt,
                                       boolean recyclable) throws IOException {
        return new CustomYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new CustomYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, _createReader(data, offset, len, null, ctxt));
    }

}

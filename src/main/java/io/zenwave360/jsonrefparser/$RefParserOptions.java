package io.zenwave360.jsonrefparser;

public class $RefParserOptions {
    public enum OnCircular {
        RESOLVE,
        SKIP,
        FAIL
    }
    
    public OnCircular onCircular = OnCircular.RESOLVE;

    public $RefParserOptions(OnCircular onCircular) {
        this.onCircular = onCircular;
    }
}

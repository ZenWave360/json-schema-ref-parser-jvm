package io.zenwave360.jsonrefparser;

public class $RefParserOptions {
    public enum OnCircular {
        RESOLVE,
        SKIP,
        FAIL
    }

    public enum OnMissing {
        SKIP,
        FAIL
    }
    
    public OnCircular onCircular = OnCircular.RESOLVE;
    public OnMissing onMissing = OnMissing.FAIL;

    public $RefParserOptions() {

    }

    @Deprecated
    public $RefParserOptions(OnCircular onCircular) {
        this.onCircular = onCircular;
    }

    public $RefParserOptions withOnCircular(OnCircular onCircular) {
        this.onCircular = onCircular;
        return this;
    }

    public $RefParserOptions withOnMissing(OnMissing onMissing) {
        this.onMissing = onMissing;
        return this;
    }

}

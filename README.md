JSON Schema $Ref Parser for the JVM
=====================================

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zenwave360/json-schema-ref-parser-jvm.svg?label=Maven%20Central&logo=apachemaven)](https://search.maven.org/artifact/io.github.zenwave360/json-schema-ref-parser-jvm)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/ZenWave360/json-schema-ref-parser-jvm?logo=GitHub)](https://github.com/ZenWave360/json-schema-ref-parser-jvm/releases)
[![build](https://github.com/ZenWave360/json-schema-ref-parser-jvm/workflows/build/badge.svg)](https://github.com/ZenWave360/json-schema-ref-parser-jvm/actions/workflows/build.yml)
[![coverage](https://raw.githubusercontent.com/ZenWave360/json-schema-ref-parser-jvm/badges/jacoco.svg)](https://github.com/ZenWave360/json-schema-ref-parser-jvm/actions/workflows/build.yml)
[![branches coverage](https://raw.githubusercontent.com/ZenWave360/json-schema-ref-parser-jvm/badges/branches.svg)](https://github.com/ZenWave360/json-schema-ref-parser-jvm/actions/workflows/build.yml)
[![GitHub](https://img.shields.io/github/license/ZenWave360/json-schema-ref-parser-jvm)](https://github.com/ZenWave360/json-schema-ref-parser-jvm/blob/main/LICENSE)


Parse, Resolve, and Dereference JSON Schema $ref pointers

This is a Java implementation of the wonderful Node.js [JSON Schema $Ref Parser](https://apitools.dev/json-schema-ref-parser/).

It also calculates [json-path -> to file location range](#calculate-json-path---to-file-location-range) for parsed files inspired in [@stoplight/yaml](https://github.com/stoplightio/yaml)

The Problem:
--------------------------

If you need to parse a JSON/Yaml based specification that uses JSON Pointers as ($ref) references like JSON Schema, OpenAPI or AsyncAPI and you find yourself writing yet another ad hoc JSON Schema parser... we got you covered!

JSON Schema $Ref Parser for Java is a full [JSON Reference](https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03) and [JSON Pointer](https://tools.ietf.org/html/rfc6901) implementation that crawls even the most complex [JSON Schemas](http://json-schema.org/latest/json-schema-core.html) returning a simple Map of nodes. Results include also for convenience a [Jayway JSONPath JsonContext](https://github.com/json-path/JsonPath/blob/master/json-path/src/main/java/com/jayway/jsonpath/internal/JsonContext.java) for reading, transversing and filtering the resulting schema.

Example:
--------------------------

```javascript
{
  "definitions": {
    "person": {
      // references an external file
      "$ref": "schemas/people/Bruce-Wayne.json"
    },
    "place": {
      // references a sub-schema in an external file
      "$ref": "schemas/places.yaml#/definitions/Gotham-City"
    },
    "thing": {
      // references a URL
      "$ref": "http://wayne-enterprises.com/things/batmobile"
    },
    "color": {
      // references a value in an external file via an internal reference
      "$ref": "#/definitions/thing/properties/colors/black-as-the-night"
    }
  }
}
```

Solution:
--------------------------

```java
File file = new File("src/test/resources/openapi/allOf.yml");
$RefParser parser = new $RefParser(file);
$Refs refs = parser.parse().dereference().mergeAllOf().getRefs();
Object resultMapOrList = refs.schema();
```

Skip (leave unresolved) circular references:

```java
$RefParser parser = new $RefParser(file)
        .withOptions(new $RefParserOptions().withOnCircular(SKIP));
$Refs refs = parser.parse().dereference().getRefs();
Assert.assertFalse(refs.circular);

```

With authentication:

```java
File file = new File("src/test/resources/openapi/http-external-refs.yml");
$RefParser parser = new $RefParser(file)
        .withAuthentication(new AuthenticationValue()
                .withHeader("Bearer", "<token>")
                .withUrlMatcher(url -> url.getHost().equals("raw.githubusercontent.com")))
        .withOptions(new $RefParserOptions().withOnCircular(SKIP));
$Refs refs = parser.parse().dereference().mergeAllOf().getRefs();
```

Calculate json-path -> to file location range:
--------------------------

```java
File file = new File("src/test/resources/openapi/allOf.yml");
$RefParser parser = new $RefParser(file).parse();
Pair<JsonLocation, JsonLocation> locations = parser.getRefs().getJsonLocationRange("$.info");
```

Installation:
--------------------------
```xml
<dependency>
  <groupId>io.github.zenwave360</groupId>
  <artifactId>json-schema-ref-parser-jvm</artifactId>
  <version>${json-schema-ref-parser-jvm.version}</version>
</dependency>
```

Features
--------------------------
- Use JSON, YAML or even Avro Schemas (avsc) — or even a mix of them!
- Fully derefence your schema producing a simple Map of nodes
- Caching: Results from remote URIs and local references are cached.
- Reference equality: Maintains object reference equality — $ref pointers to the same value always resolve to the same object instance
- Flexible: Bring your own readers for http://, file://, or use default ones.
- Authentication: Configure authentication headers or query parameters with url matchers.
- Circular references: Detects circular references, and you can `resolve` them, `skip` leaving unresolved or just `fail`.
- Merge `allOf` references into a single object.
- Back References Map: consult anytime the original reference of a given node.
- JsonPath: use [Jayway's JsonPath](https://github.com/json-path/JsonPath) to transverse/filter the resulting tree.


# License
JSON Schema $Ref Parser is 100% free and open-source, under the MIT license. Use it however you want.
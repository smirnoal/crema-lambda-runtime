package com.smirnoal.crema.events;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiGatewayProxyResponseHeadersBuilderTest {

    @Test
    void build_fluentHeaders_setsEntries() {
        var headers = ApiGatewayProxyResponseHeadersBuilder.create()
                .contentTypeJsonUtf8()
                .header("Cache-Control", "no-store")
                .build();

        assertEquals("application/json; charset=utf-8", headers.getAdditionalProperties().get("Content-Type"));
        assertEquals("no-store", headers.getAdditionalProperties().get("Cache-Control"));
    }

    @Test
    void build_headersMap_setsEntries() {
        var headers = ApiGatewayProxyResponseHeadersBuilder.create()
                .headers(Map.of("A", "1", "B", "2"))
                .build();

        assertEquals("1", headers.getAdditionalProperties().get("A"));
        assertEquals("2", headers.getAdditionalProperties().get("B"));
    }

    @Test
    void from_copiesAllEntries() {
        var headers = ApiGatewayProxyResponseHeadersBuilder.from(Map.of(
                "Content-Type", "application/json",
                "Cache-Control", "no-store"));
        assertEquals("application/json", headers.getAdditionalProperties().get("Content-Type"));
        assertEquals("no-store", headers.getAdditionalProperties().get("Cache-Control"));
    }

    @Test
    void from_emptyMap_yieldsEmptyHeaders() {
        var headers = ApiGatewayProxyResponseHeadersBuilder.from(Map.of());
        assertTrue(headers.getAdditionalProperties().isEmpty());
    }

    @Test
    void from_rejectsNullMap() {
        assertThrows(NullPointerException.class, () -> ApiGatewayProxyResponseHeadersBuilder.from(null));
    }

    @Test
    void headers_rejectsNullMap() {
        assertThrows(NullPointerException.class, () -> ApiGatewayProxyResponseHeadersBuilder.create().headers(null));
    }

    @Test
    void headers_rejectsNullKeyOrValue() {
        var nullKey = new HashMap<String, String>();
        nullKey.put(null, "x");
        assertThrows(NullPointerException.class,
                () -> ApiGatewayProxyResponseHeadersBuilder.create().headers(nullKey));

        var nullValue = new HashMap<String, String>();
        nullValue.put("k", null);
        assertThrows(NullPointerException.class,
                () -> ApiGatewayProxyResponseHeadersBuilder.create().headers(nullValue));
    }

    @Test
    void of_setsSingleHeader() {
        var headers = ApiGatewayProxyResponseHeadersBuilder.of("X-Foo", "bar");
        assertEquals("bar", headers.getAdditionalProperties().get("X-Foo"));
    }

    @Test
    void header_rejectsNulls() {
        assertThrows(NullPointerException.class,
                () -> ApiGatewayProxyResponseHeadersBuilder.create().header(null, "v"));
        assertThrows(NullPointerException.class,
                () -> ApiGatewayProxyResponseHeadersBuilder.create().header("k", null));
    }
}

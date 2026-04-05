package com.smirnoal.crema.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder and factories for {@link ApiGatewayHttpApiResponseHeaders} (HTTP API v2 response).
 * <p>
 * If your IDE shows {@code setHeaders(ApiGatewayHttpApiResponseHeaders)}, this class is the recommended way to
 * construct that generated headers object.
 * <p>
 * Event POJOs are generated from JSON Schema. Header maps use {@code additionalProperties}, which map to
 * {@link ApiGatewayHttpApiResponseHeaders#setAdditionalProperty} in generated code.
 */
public final class ApiGatewayHttpApiResponseHeadersBuilder {

    private final Map<String, String> entries = new LinkedHashMap<>();

    private ApiGatewayHttpApiResponseHeadersBuilder() {
    }

    /**
     * Starts a new fluent builder for {@link ApiGatewayHttpApiResponseHeaders}.
     *
     * <pre>{@code
     * var headers = ApiGatewayHttpApiResponseHeadersBuilder.create()
     *         .header("X-Request-Id", "abc")
     *         .build();
     * }</pre>
     */
    public static ApiGatewayHttpApiResponseHeadersBuilder create() {
        return new ApiGatewayHttpApiResponseHeadersBuilder();
    }

    /**
     * Adds one header entry.
     */
    public ApiGatewayHttpApiResponseHeadersBuilder header(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        entries.put(name, value);
        return this;
    }

    /**
     * Copies every map entry into this builder.
     */
    public ApiGatewayHttpApiResponseHeadersBuilder headers(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers");
        headers.forEach((name, value) -> header(
                Objects.requireNonNull(name, "header name"),
                Objects.requireNonNull(value, "header value for " + name)));
        return this;
    }

    /**
     * Adds {@code Content-Type: application/json; charset=utf-8}.
     */
    public ApiGatewayHttpApiResponseHeadersBuilder contentTypeJsonUtf8() {
        return header("Content-Type", "application/json; charset=utf-8");
    }

    /**
     * Builds a new generated headers object.
     */
    public ApiGatewayHttpApiResponseHeaders build() {
        var out = new ApiGatewayHttpApiResponseHeaders();
        entries.forEach(out::setAdditionalProperty);
        return out;
    }

    /**
     * Convenience factory for a single header.
     *
     * <pre>{@code
     * var headers = ApiGatewayHttpApiResponseHeadersBuilder.of("Content-Type", "application/json");
     * }</pre>
     */
    public static ApiGatewayHttpApiResponseHeaders of(String name, String value) {
        return create().header(name, value).build();
    }

    /**
     * Convenience factory that copies all entries from a map.
     *
     * <pre>{@code
     * var headers = ApiGatewayHttpApiResponseHeadersBuilder.from(Map.of("Cache-Control", "no-store"));
     * }</pre>
     */
    public static ApiGatewayHttpApiResponseHeaders from(Map<String, String> headers) {
        return create().headers(headers).build();
    }

}

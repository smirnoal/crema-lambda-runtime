package com.smirnoal.crema.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder and factories for {@link ApiGatewayProxyResponseHeaders} (REST API proxy integration response).
 * <p>
 * If your IDE shows {@code setHeaders(ApiGatewayProxyResponseHeaders)}, this class is the recommended way to
 * construct that generated headers object.
 * <p>
 * Event POJOs are generated from JSON Schema. Header maps use {@code additionalProperties}, which map to
 * {@link ApiGatewayProxyResponseHeaders#setAdditionalProperty} in generated code.
 *
 * <pre>{@code
 * var headers = ApiGatewayProxyResponseHeadersBuilder.create()
 *         .header("Content-Type", "application/json; charset=utf-8")
 *         .header("Cache-Control", "no-store")
 *         .build();
 * }</pre>
 */
public final class ApiGatewayProxyResponseHeadersBuilder {

    private final Map<String, String> entries = new LinkedHashMap<>();

    private ApiGatewayProxyResponseHeadersBuilder() {
    }

    /**
     * Starts a new fluent builder for {@link ApiGatewayProxyResponseHeaders}.
     *
     * <pre>{@code
     * var headers = ApiGatewayProxyResponseHeadersBuilder.create()
     *         .header("X-Request-Id", "abc")
     *         .build();
     * }</pre>
     */
    public static ApiGatewayProxyResponseHeadersBuilder create() {
        return new ApiGatewayProxyResponseHeadersBuilder();
    }

    /**
     * Adds one header entry.
     */
    public ApiGatewayProxyResponseHeadersBuilder header(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        entries.put(name, value);
        return this;
    }

    /**
     * Copies every map entry into this builder.
     */
    public ApiGatewayProxyResponseHeadersBuilder headers(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers");
        headers.forEach((name, value) -> header(
                Objects.requireNonNull(name, "header name"),
                Objects.requireNonNull(value, "header value for " + name)));
        return this;
    }

    /**
     * Adds {@code Content-Type: application/json; charset=utf-8}.
     */
    public ApiGatewayProxyResponseHeadersBuilder contentTypeJsonUtf8() {
        return header("Content-Type", "application/json; charset=utf-8");
    }

    /**
     * Builds a new generated headers object.
     */
    public ApiGatewayProxyResponseHeaders build() {
        var out = new ApiGatewayProxyResponseHeaders();
        entries.forEach(out::setAdditionalProperty);
        return out;
    }

    /**
     * Convenience factory for a single header.
     *
     * <pre>{@code
     * var headers = ApiGatewayProxyResponseHeadersBuilder.of("Content-Type", "application/json");
     * }</pre>
     */
    public static ApiGatewayProxyResponseHeaders of(String name, String value) {
        return create().header(name, value).build();
    }

    /**
     * Convenience factory that copies all entries from a map.
     *
     * <pre>{@code
     * var headers = ApiGatewayProxyResponseHeadersBuilder.from(Map.of("Cache-Control", "no-store"));
     * }</pre>
     */
    public static ApiGatewayProxyResponseHeaders from(Map<String, String> headers) {
        return create().headers(headers).build();
    }

}

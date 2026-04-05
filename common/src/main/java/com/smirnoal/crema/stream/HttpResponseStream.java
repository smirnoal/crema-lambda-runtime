package com.smirnoal.crema.stream;

import com.smirnoal.crema.json.JsonText;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper for Lambda Function URL integration.
 * Wraps a {@link ResponseStream} to prepend HTTP metadata (status code, headers) as a JSON
 * prelude followed by an 8-byte null delimiter before the first user write.
 * Uses content-type {@code application/vnd.awslambda.http-integration-response}.
 */
public final class HttpResponseStream {

    public static final String METADATA_PRELUDE_CONTENT_TYPE =
            "application/vnd.awslambda.http-integration-response";

    private static final int DELIMITER_LEN = 8;

    private HttpResponseStream() {
    }

    /**
     * Wrap the given stream with a metadata prelude.
     * On the first write, the prelude JSON and 8-byte null delimiter are sent before user data.
     *
     * @param stream  the underlying response stream
     * @param prelude the HTTP metadata (status code, headers, cookies)
     * @return a wrapped stream that writes the prelude on first write
     */
    public static ResponseStream from(ResponseStream stream, Prelude prelude) {
        stream.setContentType(METADATA_PRELUDE_CONTENT_TYPE);
        return new PreludeResponseStream(stream, prelude);
    }

    /**
     * Create a builder for the prelude.
     */
    public static Prelude.Builder prelude() {
        return new Prelude.Builder();
    }

    private static byte[] preludeBytes(Prelude prelude) {
        byte[] jsonBytes = serializePreludeToJson(prelude).getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[jsonBytes.length + DELIMITER_LEN];
        System.arraycopy(jsonBytes, 0, result, 0, jsonBytes.length);
        return result;
    }

    private static String serializePreludeToJson(Prelude prelude) {
        StringBuilder json = new StringBuilder();
        json.append("{\"statusCode\":").append(prelude.statusCode());
        if (!prelude.headers().isEmpty()) {
            json.append(",\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> e : prelude.headers().entrySet()) {
                if (!first) json.append(",");
                JsonText.appendJsonString(e.getKey(), json);
                json.append(':');
                JsonText.appendJsonString(e.getValue(), json);
                first = false;
            }
            json.append("}");
        }
        if (!prelude.cookies().isEmpty()) {
            json.append(",\"cookies\":[");
            for (int i = 0; i < prelude.cookies().size(); i++) {
                if (i > 0) json.append(",");
                JsonText.appendJsonString(prelude.cookies().get(i), json);
            }
            json.append("]");
        }
        json.append("}");
        return json.toString();
    }

    /**
     * HTTP metadata prelude for Function URL responses.
     *
     * @param statusCode HTTP status code
     * @param headers    response headers
     * @param cookies    Set-Cookie values
     */
    public record Prelude(int statusCode, Map<String, String> headers, List<String> cookies) {

        public static class Builder {
            private int statusCode = 200;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private final List<String> cookies = new ArrayList<>();

            public Builder statusCode(int statusCode) {
                this.statusCode = statusCode;
                return this;
            }

            public Builder header(String name, String value) {
                this.headers.put(name, value);
                return this;
            }

            public Builder cookie(String cookie) {
                this.cookies.add(cookie);
                return this;
            }

            public Prelude build() {
                return new Prelude(statusCode, Map.copyOf(headers), List.copyOf(cookies));
            }
        }
    }

    private static final class PreludeResponseStream extends ResponseStream {
        private final ResponseStream delegate;
        private final Prelude prelude;
        private boolean preludeWritten;

        PreludeResponseStream(ResponseStream delegate, Prelude prelude) {
            this.delegate = delegate;
            this.prelude = prelude;
        }

        @Override
        public void setContentType(String contentType) {
            delegate.setContentType(contentType);
        }

        private void ensurePreludeWritten() throws IOException {
            if (!preludeWritten) {
                delegate.write(HttpResponseStream.preludeBytes(prelude));
                delegate.flush();
                preludeWritten = true;
            }
        }

        @Override
        public void write(int b) throws IOException {
            ensurePreludeWritten();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            ensurePreludeWritten();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensurePreludeWritten();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }
    }
}

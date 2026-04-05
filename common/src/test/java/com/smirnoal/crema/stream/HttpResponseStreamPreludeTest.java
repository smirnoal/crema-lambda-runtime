package com.smirnoal.crema.stream;

import com.smirnoal.crema.stream.HttpResponseStream.Prelude;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseStreamPreludeTest {

    @Test
    void from_minimal_statusCodeOnly() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(200)
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.write(new byte[0]);
        wrapped.flush();

        assertEquals("{\"statusCode\":200}", capture.getPreludeJson());
        assertEquals("", capture.getBody());
        assertTrue(capture.hasValidDelimiter());
    }

    @Test
    void from_withHeaders() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(404)
                .header("Content-Type", "text/plain")
                .header("X-Custom", "value")
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.write(new byte[0]);
        wrapped.flush();

        String json = capture.getPreludeJson();
        assertTrue(json.contains("\"statusCode\":404"));
        assertTrue(json.contains("\"headers\":"));
        assertTrue(json.contains("\"Content-Type\":\"text/plain\""));
        assertTrue(json.contains("\"X-Custom\":\"value\""));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(capture.hasValidDelimiter());
    }

    @Test
    void from_withCookies() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(200)
                .cookie("session=abc123")
                .cookie("lang=en")
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.write(new byte[0]);
        wrapped.flush();

        String json = capture.getPreludeJson();
        assertTrue(json.contains("\"cookies\":"));
        assertTrue(json.contains("\"session=abc123\""));
        assertTrue(json.contains("\"lang=en\""));
    }

    @Test
    void from_headersAndCookiesWithJsonEscaping() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(200)
                .header("Set-Cookie", "name=\"value\"")
                .cookie("key=val\\ue")
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.write(new byte[0]);
        wrapped.flush();

        String json = capture.getPreludeJson();
        assertTrue(json.contains("\\\"value\\\""));
        assertTrue(json.contains("val\\\\ue"));
    }

    @Test
    void from_delimiterIsExactly8Bytes() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(200)
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.write(new byte[0]);
        wrapped.flush();

        assertTrue(capture.hasValidDelimiter());
        int delimiterStart = capture.findDelimiterStart();
        assertTrue(delimiterStart >= 0);
        assertEquals(capture.getPreludeJson().getBytes(StandardCharsets.UTF_8).length, delimiterStart);
    }

    @Test
    void from_preludeAndBodyWrittenOnFirstWrite() throws IOException {
        CapturingResponseStream capture = new CapturingResponseStream();
        Prelude prelude = HttpResponseStream.prelude()
                .statusCode(201)
                .header("Content-Type", "text/plain")
                .header("X-Custom", "value")
                .build();

        ResponseStream wrapped = HttpResponseStream.from(capture, prelude);
        wrapped.setContentType("ignored");
        wrapped.write("body".getBytes(StandardCharsets.UTF_8));
        wrapped.flush();

        String json = capture.getPreludeJson();
        assertTrue(json.contains("\"statusCode\":201"));
        assertTrue(json.contains("\"Content-Type\":\"text/plain\""));
        assertTrue(json.contains("\"X-Custom\":\"value\""));
        assertEquals("body", capture.getBody());
        assertTrue(capture.hasValidDelimiter());
    }

    private static final class CapturingResponseStream extends ResponseStream {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void setContentType(String contentType) {
            // no-op for capture
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }

        int findDelimiterStart() {
            byte[] bytes = toByteArray();
            for (int i = 0; i <= bytes.length - 8; i++) {
                boolean allNull = true;
                for (int j = 0; j < 8; j++) {
                    if (bytes[i + j] != 0) {
                        allNull = false;
                        break;
                    }
                }
                if (allNull) return i;
            }
            return -1;
        }

        String getPreludeJson() {
            int delimiterStart = findDelimiterStart();
            if (delimiterStart < 0) return "";
            return new String(toByteArray(), 0, delimiterStart, StandardCharsets.UTF_8);
        }

        String getBody() {
            int delimiterStart = findDelimiterStart();
            if (delimiterStart < 0) return "";
            byte[] bytes = toByteArray();
            int bodyStart = delimiterStart + 8;
            if (bodyStart >= bytes.length) return "";
            return new String(bytes, bodyStart, bytes.length - bodyStart, StandardCharsets.UTF_8);
        }

        boolean hasValidDelimiter() {
            int delimiterStart = findDelimiterStart();
            if (delimiterStart < 0) return false;
            byte[] bytes = toByteArray();
            for (int i = 0; i < 8; i++) {
                if (bytes[delimiterStart + i] != 0) return false;
            }
            return true;
        }
    }
}

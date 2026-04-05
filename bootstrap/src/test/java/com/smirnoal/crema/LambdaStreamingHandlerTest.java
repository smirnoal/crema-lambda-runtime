package com.smirnoal.crema;

import com.smirnoal.crema.stream.ResponseStream;
import com.smirnoal.crema.stream.StreamOnlyHandler;
import com.smirnoal.crema.stream.StreamingFunction;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LambdaStreamingHandlerTest {

    @Test
    void streamOnlyHandler_invokedWithStream_noInputDeserializer() throws Exception {
        final byte[][] captured = new byte[1][];
        ResponseStream capturingStream = new ResponseStream() {
            @Override
            public void setContentType(String contentType) {
            }

            @Override
            public void write(int b) throws IOException {
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                captured[0] = new byte[len];
                System.arraycopy(b, off, captured[0], 0, len);
            }
        };

        LambdaStreamingHandler<Void> handler = new LambdaStreamingHandler<Void>()
                .withHandler((ResponseStream rs) -> rs.write("hello".getBytes(StandardCharsets.UTF_8)));

        assertNull(handler.toInputType(new byte[0]));
        handler.handle(null, capturingStream);

        assertNotNull(captured[0]);
        assertEquals("hello", new String(captured[0], StandardCharsets.UTF_8));
    }

    @Test
    void streamOnlyHandler_toInputTypeReturnsNull() {
        LambdaStreamingHandler<Void> handler = new LambdaStreamingHandler<Void>()
                .withHandler((ResponseStream rs) -> {});
        assertNull(handler.toInputType(new byte[]{1, 2, 3}));
    }

    @Test
    void mutualExclusivity_streamOnlyThenEvent_throws() {
        LambdaStreamingHandler<String> handler = new LambdaStreamingHandler<String>()
                .withHandler((ResponseStream rs) -> {});

        assertThrows(IllegalStateException.class, () ->
                handler.withHandler((String event, ResponseStream rs) -> {}));
    }

    @Test
    void mutualExclusivity_eventThenStreamOnly_throws() {
        LambdaStreamingHandler<String> handler = new LambdaStreamingHandler<String>()
                .withInputTypeDeserializer(String::new)
                .withHandler((String event, ResponseStream rs) -> {});

        assertThrows(IllegalStateException.class, () ->
                handler.withHandler((ResponseStream rs) -> {}));
    }
}

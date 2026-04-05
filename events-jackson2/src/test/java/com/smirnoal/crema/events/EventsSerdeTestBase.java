package com.smirnoal.crema.events;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

abstract class EventsSerdeTestBase {

    protected static String loadResource(String path) throws IOException {
        try (InputStream is = EventsSerdeTestBase.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

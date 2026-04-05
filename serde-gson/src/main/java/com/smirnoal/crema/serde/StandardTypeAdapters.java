package com.smirnoal.crema.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

/**
 * Standard Gson type adapters for common types used in Lambda events.
 * Use with {@link GsonLambdaSerde#defaultGsonBuilder()} or register directly on a custom {@link com.google.gson.GsonBuilder}.
 */
public final class StandardTypeAdapters {

    private StandardTypeAdapters() {
    }

    /** Adapter for {@link Instant} (ISO-8601 string format). */
    public static final TypeAdapter<Instant> INSTANT = new InstantTypeAdapter();

    private static final class InstantTypeAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value != null ? value.toString() : null);
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            String s = in.nextString();
            return s != null ? Instant.parse(s) : null;
        }
    }
}

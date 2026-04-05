package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaRegistryDriftTest {

    private static final Pattern SCHEMA_REF_PATTERN = Pattern.compile("\\]\\(src/main/resources/schemas/([a-zA-Z0-9\\-\\.]+\\.json)\\)");

    @Test
    void schemaRegistryMatchesSchemaFiles() throws IOException {
        Path schemasDir = Paths.get("src/main/resources/schemas");
        Path readmePath = Paths.get("README.md");

        Set<String> schemaFiles = new HashSet<>();
        try (var stream = Files.list(schemasDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .forEach(schemaFiles::add);
        }

        String readme = Files.readString(readmePath);
        String registrySection = extractEventsRegistrySection(readme);
        Set<String> documented = extractSchemaRefsFromSection(registrySection);

        Set<String> undocumented = new HashSet<>(schemaFiles);
        undocumented.removeAll(documented);

        Set<String> missing = new HashSet<>(documented);
        missing.removeAll(schemaFiles);

        List<String> failures = new ArrayList<>();
        if (!undocumented.isEmpty()) {
            String files = String.join(", ", new TreeSet<>(undocumented));
            failures.add("Mismatch: schema file(s) exist but are not in README Events Registry. File(s): " + files + ". Location: src/main/resources/schemas/. Action: add table row(s) for these schemas.");
        }
        if (!missing.isEmpty()) {
            String files = String.join(", ", new TreeSet<>(missing));
            failures.add("Mismatch: README Events Registry references non-existent schema file(s). File(s): " + files + ". Location: README.md # Events Registry. Action: remove these entries or add the file(s) to src/main/resources/schemas/.");
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    private static String extractEventsRegistrySection(String readme) {
        int start = readme.indexOf("# Events Registry");
        if (start < 0) {
            throw new IllegalStateException("README must contain '# Events Registry' section");
        }
        int contentStart = readme.indexOf('\n', start) + 1;
        int nextHeading = readme.indexOf("\n# ", contentStart);
        int contentEnd = nextHeading > 0 ? nextHeading : readme.length();
        return readme.substring(contentStart, contentEnd);
    }

    private static Set<String> extractSchemaRefsFromSection(String section) {
        Set<String> refs = new HashSet<>();
        Matcher m = SCHEMA_REF_PATTERN.matcher(section);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }
}

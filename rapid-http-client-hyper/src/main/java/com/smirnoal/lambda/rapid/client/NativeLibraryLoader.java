package com.smirnoal.lambda.rapid.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Loads {@code librapid_http} from {@code /var/task/lib/native}, {@code RAPID_HTTP_NATIVE_DIR},
 * or system property {@code rapid.http.native.dir}. Does not extract from JAR.
 */
final class NativeLibraryLoader {

    private static final String ENV_NATIVE_DIR = "RAPID_HTTP_NATIVE_DIR";
    private static final String PROP_NATIVE_DIR = "rapid.http.native.dir";
    private static final String DEFAULT_LINUX_DIR = "/var/task/lib/native";

    private NativeLibraryLoader() {
    }

    static void load() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            throw new UnsatisfiedLinkError("rapid-http-client-hyper supports Linux only. os.name=" + os);
        }

        String dir = firstNonBlank(
                System.getProperty(PROP_NATIVE_DIR),
                System.getenv(ENV_NATIVE_DIR));
        if (dir != null) {
            File f = new File(dir, libraryFileName());
            if (!f.isFile()) {
                throw new UnsatisfiedLinkError("Native library not found: " + f.getAbsolutePath());
            }
            System.load(f.getAbsolutePath());
            return;
        }

        Path defaultPath = Path.of(DEFAULT_LINUX_DIR, libraryFileName());
        if (!Files.isRegularFile(defaultPath)) {
            throw new UnsatisfiedLinkError(
                    "Native library not found at " + defaultPath.toAbsolutePath()
                            + ". Set " + ENV_NATIVE_DIR + " or -D" + PROP_NATIVE_DIR + ".");
        }
        System.load(defaultPath.toAbsolutePath().toString());
    }

    static String libraryFileName() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if ("amd64".equals(arch) || "x86_64".equals(arch)) {
            return "librapid_http_amd64.so";
        }
        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            return "librapid_http_arm64.so";
        }
        throw new UnsatisfiedLinkError("Unsupported Linux architecture for rapid-http-hyper: " + arch);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}

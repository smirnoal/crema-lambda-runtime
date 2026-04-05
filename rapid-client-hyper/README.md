# rapid-client-hyper

Lambda Runtime API HTTP client: `**/runtime/invocation/next**`, buffered `**.../response**`, and **streaming `.../response`** (chunked + trailers, same contract as the Netty client) are implemented in Rust ([hyper](https://hyper.rs/)) and called via JNI. Other `LambdaRapidHttpClient` methods lazily delegate to `[JdkHttpClientProvider](../rapid-client-jdk/src/main/java/com/smirnoal/crema/rapid/client/JdkHttpClientProvider.java)`.

See [design-http-client-rust.md](../../doc/design-http-client-rust.md).

## Native library

At runtime the JVM loads `**libcrema_rapid_hyper**` from disk:


| Resolution order | Mechanism                                                                                                                                                         |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1                | `**CREMA_RAPID_HYPER_NATIVE_DIR**` or system property `**crema.rapid.hyper.native.dir**`: directory containing `libcrema_rapid_hyper_amd64.so` or `libcrema_rapid_hyper_arm64.so` |
| 2                | Linux default: `**/var/task/lib/native/libcrema_rapid_hyper_<arch>.so**`                                                                                         |


The published JAR now embeds two Linux natives:

- `native/libcrema_rapid_hyper_amd64.so`
- `native/libcrema_rapid_hyper_arm64.so`

Image build should unpack the current arch entry and install it to `**/var/task/lib/native/libcrema_rapid_hyper_<arch>.so**`.

## Building the Rust crate

From this directory:

```bash
cd ..
./gradlew :rapid-client-hyper:publishToMavenLocal
```

Gradle builds host-native Rust for tests via `compileRust`, and builds two Linux release libraries for packaging with Docker:

- `native/libcrema_rapid_hyper_amd64.so` (from `--platform linux/amd64`)
- `native/libcrema_rapid_hyper_arm64.so` (from `--platform linux/arm64`)

## Gradle / tests

```bash
../gradlew :rapid-client-hyper:test
```

Requires **Rust** on `PATH`. Tests set `**CREMA_RAPID_HYPER_NATIVE_DIR`** to `rust/target/.../debug` (or `release` with `-PrustRelease`).

JNI tests (including streaming) are `**@EnabledOnOs(OS.LINUX)**` because the published native is Linux-only. On macOS, run `**./gradlew :rapid-client-hyper:testLinux**` (Docker) or rely on `**cargo test**` in `rust/` for pure-Rust coverage.

### `testLinux` (Docker)

First run builds a small image (`**buildTestLinuxImage**`) with Corretto 17 + Rust **1.89.0**; the Dockerfile also runs `**./gradlew --version`** so the **Gradle wrapper distribution** is stored under `**/root/.gradle**` in the image and is not downloaded again on each `**testLinux**` run. The image build uses the **Gradle repo root** as context (see `**.dockerignore**` at the repo root).

**Mounts:** only the repo root is shared into the container as `**/work`** (on Mac, `**cached**` consistency). Java/Rust outputs under `**/work/**` (e.g. `**rust/target**`, module `**build/**` dirs) land on the host. **Cargo** uses `**/root/.cargo**` in the container (not on the mount); **Gradle dependency** caches under `**/root/.gradle/caches**` are still per-container unless you warm them in the image.

**Why it can look “stuck” with low CPU (`docker stats`):** Gradle and Cargo write many small files through the bind mount; on Docker Desktop (especially Mac) that I/O often waits on the VM sync layer rather than burning CPU.

**Low CPU during the test phase** is normal (short JNI + MockWebServer I/O).

If you previously used named `**rapid-hyper-test-linux-*`** volumes, remove them when cleaning up: `**docker volume ls | grep rapid-hyper-test-linux**` then `**docker volume rm <name>**`.

**Apple Silicon:** the default platform is `**linux/arm64`** (no QEMU). Use `**-PtestLinuxPlatform=linux/amd64**` to match x86_64 Lambda / CI (slower locally on M-series).


| Property            | Default                                      | Purpose                                    |
| ------------------- | -------------------------------------------- | ------------------------------------------ |
| `testLinuxPlatform` | `linux/arm64` on Mac ARM; else `linux/amd64` | `docker build` / `docker run` `--platform` |
| `testLinuxImage`    | `rapid-hyper-test-linux:local`               | Image tag                                  |


Explicit image rebuild: `**./gradlew :rapid-client-hyper:buildTestLinuxImage**`

## Streaming

`startStreamingResponse` uses the same Runtime API streaming POST as `rapid-client-netty` (mode header, chunked body, error trailers). Trailer JSON/Base64 is produced on the Java side to stay aligned with the Netty client.

## Provider priority

`[HyperHttpClientProvider](src/main/java/com/smirnoal/crema/rapid/client/HyperHttpClientProvider.java)` `**priority()**` is **15** (above JDK and OkHttp, below Netty).

To use this client, add the artifact to the function classpath **in place of** or **in addition to** other `rapid-client-*` modules; only the highest-priority `LambdaRapidHttpClientProvider` is selected.

## User-Agent

Native requests use `**com.smirnoal.crema-hyper/<crate version>`** (includes `**crema-hyper**`). Delegated JDK calls keep `com.smirnoal.crema-jdk/...`.
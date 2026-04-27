# Crema (crema-lambda-runtime)

**Crema** is a custom AWS Lambda runtime for **Java**. It lets Java Lambda functions start from a normal `main` method, giving you explicit control over bootstrap, classpath, handler wiring, serialization, logging, and deployment shape without depending on the managed Java runtime to load your handler.

Use Crema when you want more control than the managed Java runtime gives you, while still writing straightforward, modern Java:

- **Multiple deployment paths**: run as a managed-runtime ZIP with `AWS_LAMBDA_EXEC_WRAPPER`, as a custom container image, or as a custom `provided.al2023` runtime.
- **First-class GraalVM support**: Crema modules include native-image reachability metadata so Java Lambda functions can compile to native binaries without treating GraalVM as an afterthought.
- **Pluggable serialization**: choose string, Jackson, Gson, or custom serializers/deserializers through the `LambdaSerde` API instead of hard-wiring one JSON stack.
- **Real structured logging**: unlike the managed runtime’s fixed JSON fields, Crema can add arbitrary top-level fields to log records and embed structured JSON payloads, including formats such as CloudWatch Embedded Metric Format (EMF).

Compared with the managed Java runtime, Crema moves the runtime bootstrap into your application package. AWS still provides the Lambda execution environment and Runtime API; Crema provides the Java Runtime Interface Client (RIC) loop, handler wiring, serde helpers, and deployment building blocks.

## Minimal example

### Gradle

`groupId` is **`com.smirnoal.crema`**. Check the most recent version on Maven Central.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.smirnoal.crema:bootstrap:0.1.0")
}
```

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>com.smirnoal.crema</groupId>
    <artifactId>bootstrap</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

Handler entrypoint:

```java
import com.smirnoal.crema.LambdaApplication;
import com.smirnoal.crema.LambdaHandler;
import com.smirnoal.crema.serde.StringSerde;

public class HelloHandler {
    public static void main(String[] args) {
        var handler = new LambdaHandler<String, String>()
                .withLambdaSerde(new StringSerde())
                .withHandler(event -> "Hello, " + event);
        new LambdaApplication().run(handler);
    }
}
```

## When to consider Crema

Consider Crema when you want to write modern Java Lambda functions with response streaming, structured logging, pluggable serialization, schema-first event types, and GraalVM native-image support for Crema modules.

SnapStart is supported in the managed Java runtime deployment mode with a wrapper script. AWS SnapStart is limited to supported managed runtimes; OS-only runtimes and container images are not supported.

Crema also provides a schema-first events library generated from JSON Schemas, with Jackson-based serde for standard Lambda events. This keeps event model updates straightforward. Please open an issue if a supported AWS event shape is missing or outdated.

## Modules

| Module             | Description                                                 |
| ------------------ | ----------------------------------------------------------- |
| common             | Shared types and interfaces used across runtime modules     |
| events-schemas     | JSON Schemas for AWS Lambda event/response payloads         |
| events-jackson2    | Jackson 2 event model classes and helper APIs               |
| serde-jackson2     | Jackson 2 serializers/deserializers for runtime integration |
| serde-gson         | Gson serializers/deserializers for runtime integration      |
| rapid-client-jdk   | Runtime API HTTP client (`java.net.http`)                   |
| rapid-client-netty | Runtime API HTTP client (Netty; streaming)                  |
| rapid-client-hyper | Runtime API HTTP client (Rust + JNI; experimental)          |
| bootstrap          | Runtime bootstrap that wires everything together            |

Typical consumer dependency: **`bootstrap`** (pulls `common` + `rapid-client-jdk`). Add **`events-jackson2`** when using typed AWS events. Use **`rapid-client-netty`** or **`rapid-client-hyper`** when you need those transports (see each module’s docs).

## Deployment options

### ZIP (managed Java runtime)

Deploy a ZIP to Lambda with a supported managed Java runtime. Set `AWS_LAMBDA_EXEC_WRAPPER` to Crema’s provided `bootstrap.sh`, which sets `CLASSPATH` and runs your `main`. Lambda’s `_HANDLER` should be your main class.

Choose this path first for normal JVM deployments, when you want the smallest packaging change from managed Java Lambda, or when you need AWS SnapStart.

### ZIP (`provided.al2023` + GraalVM native image)

Build a **native** executable named `bootstrap` that includes the RIC loop and handler. No JVM on Lambda; typically **faster cold starts**. Requires **native-image** configuration (this repo ships `META-INF/native-image` for Crema modules; your handler may need more).

Choose this path for cold-start-sensitive functions where native-image build time, reachability metadata, and native debugging are acceptable tradeoffs. SnapStart does not apply to OS-only runtimes.

### Docker (container image)

Package Crema + handler in an image with your chosen JVM. Deploy as a Lambda container image.

Choose this path when you need OS packages, native libraries, a specific JVM distribution, or fully reproducible image-based deployment.

### Lambda Layer

Publish JARs as a layer; functions attach the layer and bundle only application code.

Choose this path when several ZIP functions should share the same Crema runtime artifacts. Keep layer versioning and rollout coordination in mind; bundling dependencies directly is simpler for one-off functions.

## License

This project is licensed under **Elastic License 2.0 (`ELv2`)**. See `LICENSE`.

In short:

- Internal and self-hosted use is allowed, including commercial use.
- You may not offer Crema as a public managed runtime service for third parties under ELv2.
- If you want that kind of public managed service, contact the licensor for commercial terms (`COMMERCIAL-LICENSE.md`).

Details: `LICENSE-FAQ.md`.

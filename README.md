# Crema (crema-lambda-runtime)

Custom AWS Lambda Runtime Interface Client for Java.

## License

This project is licensed under Elastic License 2.0 (`ELv2`). See `LICENSE`.

In short:

- Internal and self-hosted use is allowed, including commercial use.
- You may not offer Crema as a public managed runtime service
  for third parties under ELv2.
- If you want to offer that kind of public managed service, contact the
  licensor for commercial terms.

For details and examples, see `LICENSE-FAQ.md`. For commercial terms, see
`COMMERCIAL-LICENSE.md`.

## Modules

| Module             | Description                                                   |
|--------------------|---------------------------------------------------------------|
| common             | Shared types and interfaces used across runtime modules     |
| events-schemas     | JSON Schemas for AWS Lambda event/response payloads           |
| events-jackson2    | Jackson 2 event model classes and helper APIs                |
| serde-jackson2     | Jackson 2 serializers/deserializers for runtime integration  |
| serde-gson         | Gson serializers/deserializers for runtime integration     |
| rapid-client-jdk   | Runtime API HTTP client (java.net.http)                        |
| rapid-client-netty | Runtime API HTTP client (Netty, supports streaming)          |
| rapid-client-hyper | Runtime API HTTP client (Rust + JNI for `/next` and `/response`) |
| bootstrap          | Runtime bootstrap that wires everything together             |

## Build

```bash
./gradlew build
./gradlew publishToMavenLocal   # makes com.smirnoal.crema:bootstrap:1.0-SNAPSHOT available to consumers
```

## Deployment options

### ZIP (managed Java runtime)

Deploy a ZIP package to a Lambda function using a managed runtime like `java17` or `java21`. The runtime provides a JVM at `/var/lang/bin/java`. Use `AWS_LAMBDA_EXEC_WRAPPER` to point to a `bootstrap.sh` script that sets the classpath and runs the handler's main class.

### ZIP (provided + GraalVM native image)

Deploy a GraalVM native binary as a `provided.al2023` function. The binary is named `bootstrap` and acts as both the runtime and the handler. No JVM needed on Lambda -- results in significantly faster cold starts.

### Docker (custom image)

Package the runtime and handler into a Docker image based on any Java distribution. Deploy as a Lambda container image. Useful when you need full control over the JVM version, system libraries, or base OS.

### Lambda Layer

Publish the bootstrap library as a Lambda Layer. Functions attach the layer and reference the runtime classes from it. This avoids bundling the runtime into every function's deployment package.

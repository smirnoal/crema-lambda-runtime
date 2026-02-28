# smirnoal-ric

Custom AWS Lambda Runtime Interface Client for Java.

## Modules

| Module | Description |
|---|---|
| `common` | Shared types and interfaces |
| `rapid-http-client` | HTTP client for the Lambda Runtime API |
| `bootstrap` | Runtime bootstrap that wires everything together |

## Build

```bash
./gradlew build
./gradlew publishToMavenLocal   # makes com.smirnoal:bootstrap:1.0-SNAPSHOT available to consumers
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

## Tests

See [smirnoal-ric-tests/](../smirnoal-ric-tests/) for container-based integration tests and AWS E2E tests.

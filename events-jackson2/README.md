# events-jackson2

Jackson-backed POJOs and `EventsJacksonLambdaSerde` for AWS Lambda event types. Generates event classes from JSON schemas in `events-schemas` via jsonschema2pojo.

## API Gateway Response Headers

Generated API Gateway response events expose typed headers (`ApiGatewayProxyResponseHeaders` for REST and
`ApiGatewayHttpApiResponseHeaders` for HTTP API). Use builder helpers for IDE-friendly construction when you see
`setHeaders(...)` signatures:

```java
var restHeaders = ApiGatewayProxyResponseHeadersBuilder.create()
        .contentTypeJsonUtf8()
        .header("Cache-Control", "no-store")
        .build();

var httpApiHeaders = ApiGatewayHttpApiResponseHeadersBuilder.of(
        "X-Request-Id", "abc-123");
```

Both builders also support `from(Map<String, String>)`, and fluent `contentTypeJsonUtf8()` on the builder instance.

## Adding Event Support

When a new event type is added to `events-schemas`, a **serde test is required** in this module.

**Prerequisite:** The schema and sample must exist in `events-schemas`. See [events-schemas README](../events-schemas/README.md#adding-new-events) for that procedure.

### Add a Serde Test

1. Add a test in the appropriate `*EventsSerdeTest` class:
   - `AuthEventsSerdeTest` — API Gateway authorizers, Cognito, AppSync
   - `ApiGatewayEventsSerdeTest` — API Gateway REST/HTTP API, WebSocket
   - `MessagingEventsSerdeTest` — SQS, SNS
   - `StorageEventsSerdeTest` — S3, S3 batch, S3 Object Lambda
   - `StreamsEventsSerdeTest` — Kinesis, DynamoDB, Firehose, time-window
   - `ComputeEventsSerdeTest` — Scheduled, ALB, EventBridge, CloudFront, Lambda Destination
   - `CloudWatchEventsSerdeTest` — CloudWatch Logs, metric/composite alarm
   - `AwsServiceEventsSerdeTest` — Connect, Lex, CodeCommit, CodePipeline, CloudFormation, Config, Secrets Manager
   - `MessagingQueuesEventsSerdeTest` — Kafka, ActiveMQ, RabbitMQ
   - `IoTEventsSerdeTest` — IoT button, IoT custom authorizer
   - `SesEventsSerdeTest` — SES
   - `VpcLatticeEventsSerdeTest` — VPC Lattice v2

2. Use the standard pattern: deserialize sample JSON, assert key fields, round-trip serialize/deserialize.

Example:

```java
@Test
void roundTripMyNewEvent() throws IOException {
    var serde = EventsJacksonLambdaSerde.forEventAndResponse(MyNewEvent.class, MyNewEvent.class);
    String json = loadResource("events/my-new-event-sample.json");
    byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

    MyNewEvent deserialized = serde.inputDeserializer().apply(inputBytes);
    assertNotNull(deserialized);
    // assert key fields

    byte[] outputBytes = serde.outputSerializer().apply(deserialized);
    MyNewEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
    assertEquals(deserialized.getSomeField(), roundTripped.getSomeField());
}
```

3. Sample files are loaded from the `events-schemas` tests JAR (`events/` resource path). The path in `loadResource()` should be `events/<sample-file>.json`.

4. Run `./gradlew :events-jackson2:test` to verify.

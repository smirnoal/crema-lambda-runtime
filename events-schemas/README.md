# events-schemas

JSON schemas for AWS Lambda event types. Part of Crema (`crema-lambda-runtime`). Schemas are consumed by events-jackson2 for POJO codegen and validation.

## Adding New Events

This guide describes how to add support for a new Lambda event type in this module. Serde test coverage is **required** for events-jackson2; see that module's README for how to add tests.

### Verification Checklist (Before and After Schema Work)

**1. Official AWS documentation (primary source)**

- [ ] Locate the canonical AWS doc for the event (e.g. [with-sqs.html](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html), service-specific docs).
- [ ] Compare your schema against the documented structure: field names, types, required vs optional.
- [ ] Prefer AWS docs over third-party sources. Field casing (e.g. `Records` vs `records`) must match what AWS sends.
- [ ] Use `additionalProperties: true` for nested objects when the docs describe flexible structures (e.g. `Details` in Connect events).

**2. Managed runtime (secondary reference)**

- [ ] Compare with `aws-lambda-java-events` (main and `events-v4-serialization-v2` if applicable).
- [ ] Check [aws/aws-lambda-java-libs](https://github.com/aws/aws-lambda-java-libs) for related issues (deserialization bugs, missing fields).
- [ ] Managed runtime uses different naming/mixins; our schemas use `@JsonProperty` via jsonschema2pojo. Match the **wire format** (JSON keys), not necessarily Java names.

**3. Sample payload**

- [ ] Use a realistic payload (from AWS docs, sample events, or aws-lambda-java-events tests).
- [ ] Ensure the sample conforms to your schema — `EventSamplesSchemaValidationTest` will enforce this.

### Step-by-Step Procedure

**1. Create the schema**

- Add `src/main/resources/schemas/<event-name>.json`.
- Use `$schema: "http://json-schema.org/draft-07/schema#"`.
- Set `title` to the Java class name (jsonschema2pojo uses `useTitleAsClassname`).
- Match AWS wire format: PascalCase where AWS uses it (e.g. `Records`, `eventSourceARN`).
- For binary fields (SQS `messageAttributes.binaryValue`, Kinesis `data`, DynamoDB `B`/`BS`): use `contentEncoding: "base64"` and `existingJavaType: "java.nio.ByteBuffer"`.
- For timestamps: use `type: "string"` with `format: "date-time"`; jsonschema2pojo maps to `java.time.Instant`.

**2. Create sample JSON**

- Add `src/test/resources/events/<event-name>-sample.json` (or `-event-sample.json` by convention).
- Keep it realistic and conformant to the schema.
- Run `./gradlew :events-schemas:spotlessApply` if needed.

**3. Update EventSamplesSchemaValidationTest**

- Add a row to the `@CsvSource` in `EventSamplesSchemaValidationTest`:
  - Format: `events/<sample-file>.json|schemas/<schema-file>.json`
- Example: `events/my-new-event-sample.json|schemas/my-new-event.json`
- Run `./gradlew :events-schemas:test` to verify the sample validates.

**4. Update Events Registry in README.md (required)**

- Add a row to the Events Registry table (below).
- Format: `| [schema-file.json](src/main/resources/schemas/schema-file.json) | Description | [AWS doc link](url) |`
- **SchemaRegistryDriftTest** fails if schema files and README rows are out of sync. Adding the row is mandatory.

**5. Add serde test in events-jackson2**

- Serde test coverage is required. See [events-jackson2 README](../events-jackson2/README.md#adding-event-support) for the procedure and test pattern.

### Schema Conventions

| Convention              | Notes                                                                 |
| ----------------------- | --------------------------------------------------------------------- |
| `title`                 | Java class name; jsonschema2pojo `useTitleAsClassname = true`         |
| PascalCase vs camelCase | Match AWS wire format (often PascalCase at top level, camelCase nested) |
| Binary fields           | `contentEncoding: "base64"`, `existingJavaType: "java.nio.ByteBuffer"` |
| Timestamps              | `format: "date-time"` → `java.time.Instant`                           |
| Forward compatibility   | `additionalProperties: true` where AWS structure is flexible          |
| Polymorphic fields      | `oneOf` when AWS sends multiple shapes (e.g. ScheduledEvent `detail`)  |

### Files to Touch (this module)

| File                                                                 | Required |
| -------------------------------------------------------------------- | -------- |
| `schemas/<name>.json`                                                | Yes      |
| `events/<name>-sample.json` (in `src/test/resources/`)               | Yes      |
| `EventSamplesSchemaValidationTest.java` (add CsvSource row)          | Yes      |
| `README.md` (Events Registry table)                                  | Yes      |

### References

- [AWS Lambda event structure](https://docs.aws.amazon.com/lambda/latest/dg/lambda-services.html)
- [aws-lambda-java-libs](https://github.com/aws/aws-lambda-java-libs) — managed runtime events

---

## Events Registry

| Schema                                                                                                                 | Description | AWS docs |
|------------------------------------------------------------------------------------------------------------------------| ----------- | -------- |
| [activemq.json](src/main/resources/schemas/activemq.json)                                                              | Amazon MQ (ActiveMQ) event | [with-mq.html](https://docs.aws.amazon.com/lambda/latest/dg/with-mq.html) |
| [alb-request.json](src/main/resources/schemas/alb-request.json)                                                                           | Application Load Balancer request | [services-alb.html](https://docs.aws.amazon.com/lambda/latest/dg/services-alb.html) |
| [alb-response.json](src/main/resources/schemas/alb-response.json)                                                                         | Application Load Balancer response | [services-alb.html](https://docs.aws.amazon.com/lambda/latest/dg/services-alb.html) |
| [apigw-custom-authorizer.json](src/main/resources/schemas/apigw-custom-authorizer.json)                                                   | API Gateway REST custom authorizer request | [api-gateway-lambda-authorizer-input.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-lambda-authorizer-input.html) |
| [apigw-custom-authorizer-response.json](src/main/resources/schemas/apigw-custom-authorizer-response.json)                                 | API Gateway REST custom authorizer response | [api-gateway-lambda-authorizer-output.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-lambda-authorizer-output.html) |
| [apigw-httpapi-request.json](src/main/resources/schemas/apigw-httpapi-request.json)                                                       | API Gateway HTTP API request | [http-api-develop-integrations-lambda.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html) |
| [apigw-httpapi-response.json](src/main/resources/schemas/apigw-httpapi-response.json)                                                     | API Gateway HTTP API response | [http-api-develop-integrations-lambda.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html) |
| [apigw-rest-request.json](src/main/resources/schemas/apigw-rest-request.json)                                                             | API Gateway REST API request | [set-up-lambda-proxy-integrations.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html) |
| [apigw-rest-response.json](src/main/resources/schemas/apigw-rest-response.json)                                                           | API Gateway REST API response | [set-up-lambda-proxy-integrations.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html) |
| [apigw-v2-custom-authorizer.json](src/main/resources/schemas/apigw-v2-custom-authorizer.json)                                             | API Gateway HTTP API custom authorizer request | [http-api-lambda-authorizer.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-lambda-authorizer.html) |
| [apigw-v2-custom-authorizer-response.json](src/main/resources/schemas/apigw-v2-custom-authorizer-response.json)                           | API Gateway HTTP API custom authorizer response | [api-gateway-lambda-authorizer-output.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-lambda-authorizer-output.html) |
| [apigw-v2-websocket-event.json](src/main/resources/schemas/apigw-v2-websocket-event.json)                                                 | API Gateway WebSocket event | [apigateway-websocket-api-lambda-integrations.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-websocket-api-lambda-integrations.html) |
| [apigw-v2-websocket-response.json](src/main/resources/schemas/apigw-v2-websocket-response.json)                                           | API Gateway WebSocket response | [set-up-lambda-proxy-integrations.html](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html) |
| [appsync-authorizer-event.json](src/main/resources/schemas/appsync-authorizer-event.json)                                                 | AppSync Lambda authorizer request | [security-authz.html](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html) |
| [appsync-authorizer-response.json](src/main/resources/schemas/appsync-authorizer-response.json)                                           | AppSync Lambda authorizer response | [security-authz.html](https://docs.aws.amazon.com/appsync/latest/devguide/security-authz.html) |
| [cloudformation-custom-resource.json](src/main/resources/schemas/cloudformation-custom-resource.json)                                     | CloudFormation custom resource | [template-custom-resources-lambda.html](https://docs.aws.amazon.com/cloudformation/latest/UserGuide/template-custom-resources-lambda.html) |
| [cloudfront.json](src/main/resources/schemas/cloudfront.json)                                                                             | CloudFront Lambda@Edge | [lambda-event-structure.html](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/lambda-event-structure.html) |
| [cloudwatch-composite-alarm.json](src/main/resources/schemas/cloudwatch-composite-alarm.json)                                             | CloudWatch Composite Alarm | [cloudwatch-and-eventbridge.html](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch-and-eventbridge.html) |
| [cloudwatch-logs.json](src/main/resources/schemas/cloudwatch-logs.json)                                                                   | CloudWatch Logs subscription | [SubscriptionFilters.html#LambdaFunctionExample](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html#LambdaFunctionExample) |
| [cloudwatch-metric-alarm.json](src/main/resources/schemas/cloudwatch-metric-alarm.json)                                                   | CloudWatch Metric Alarm | [cloudwatch-and-eventbridge.html](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch-and-eventbridge.html) |
| [codecommit.json](src/main/resources/schemas/codecommit.json)                                                                             | CodeCommit event | [how-to-notify-lambda-cc.html](https://docs.aws.amazon.com/codecommit/latest/userguide/how-to-notify-lambda-cc.html) |
| [codepipeline.json](src/main/resources/schemas/codepipeline.json)                                                                         | CodePipeline event | [actions-invoke-lambda-function.html](https://docs.aws.amazon.com/codepipeline/latest/userguide/actions-invoke-lambda-function.html) |
| [cognito-event.json](src/main/resources/schemas/cognito-event.json)                                                                       | Cognito Identity Pools sync trigger | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-create-auth-challenge.json](src/main/resources/schemas/cognito-user-pool-create-auth-challenge.json)                   | Cognito User Pool CreateAuthChallenge | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-custom-message.json](src/main/resources/schemas/cognito-user-pool-custom-message.json)                                 | Cognito User Pool CustomMessage | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-custom-sms-sender.json](src/main/resources/schemas/cognito-user-pool-custom-sms-sender.json)                           | Cognito User Pool Custom SMS Sender | [user-pool-lambda-custom-sms-sender.html](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-custom-sms-sender.html) |
| [cognito-user-pool-define-auth-challenge.json](src/main/resources/schemas/cognito-user-pool-define-auth-challenge.json)                   | Cognito User Pool DefineAuthChallenge | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-inbound-federation.json](src/main/resources/schemas/cognito-user-pool-inbound-federation.json)                         | Cognito User Pool Inbound Federation | [user-pool-lambda-inbound-federation.html](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-inbound-federation.html) |
| [cognito-user-pool-migrate-user.json](src/main/resources/schemas/cognito-user-pool-migrate-user.json)                                     | Cognito User Pool MigrateUser | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-post-authentication.json](src/main/resources/schemas/cognito-user-pool-post-authentication.json)                       | Cognito User Pool PostAuthentication | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-post-confirmation.json](src/main/resources/schemas/cognito-user-pool-post-confirmation.json)                           | Cognito User Pool PostConfirmation | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-pre-authentication.json](src/main/resources/schemas/cognito-user-pool-pre-authentication.json)                         | Cognito User Pool PreAuthentication | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-pre-signup.json](src/main/resources/schemas/cognito-user-pool-pre-signup.json)                                         | Cognito User Pool PreSignUp | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [cognito-user-pool-pre-token-generation.json](src/main/resources/schemas/cognito-user-pool-pre-token-generation.json)                     | Cognito User Pool PreTokenGeneration v1 | [user-pool-lambda-pre-token-generation.html](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html) |
| [cognito-user-pool-pre-token-generation-v2.json](src/main/resources/schemas/cognito-user-pool-pre-token-generation-v2.json)               | Cognito User Pool PreTokenGeneration v2 | [user-pool-lambda-pre-token-generation.html](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html) |
| [cognito-user-pool-verify-auth-challenge-response.json](src/main/resources/schemas/cognito-user-pool-verify-auth-challenge-response.json) | Cognito User Pool VerifyAuthChallengeResponse | [cognito-events.html](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html) |
| [config.json](src/main/resources/schemas/config.json)                                                                                     | AWS Config rule | [governance-config.html](https://docs.aws.amazon.com/lambda/latest/dg/governance-config.html) |
| [connect-event.json](src/main/resources/schemas/connect-event.json)                                                                       | Amazon Connect ContactFlowEvent | [connect-lambda-functions.html](https://docs.aws.amazon.com/connect/latest/adminguide/connect-lambda-functions.html) |
| [dynamodb-streams.json](src/main/resources/schemas/dynamodb-streams.json)                                                                 | DynamoDB Streams event | [with-ddb.html](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html) |
| [dynamodb-time-window.json](src/main/resources/schemas/dynamodb-time-window.json)                                                         | DynamoDB Streams time window | [services-ddb-windows.html](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-windows.html) |
| [eventbridge.json](src/main/resources/schemas/eventbridge.json)                                                                           | EventBridge event | [eb-what-is.html](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html) |
| [iot-button.json](src/main/resources/schemas/iot-button.json)                                                                             | IoT button event | [services-iot.html](https://docs.aws.amazon.com/lambda/latest/dg/services-iot.html) |
| [iot-custom-authorizer-event.json](src/main/resources/schemas/iot-custom-authorizer-event.json)                                           | IoT custom authorizer | [custom-auth-lambda.html](https://docs.aws.amazon.com/iot/latest/developerguide/custom-auth-lambda.html) |
| [kafka.json](src/main/resources/schemas/kafka.json)                                                                                       | Kafka / MSK event | [with-kafka.html](https://docs.aws.amazon.com/lambda/latest/dg/with-kafka.html) |
| [kinesis.json](src/main/resources/schemas/kinesis.json)                                                                                   | Kinesis Data Streams event | [with-kinesis.html](https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html) |
| [kinesis-firehose.json](src/main/resources/schemas/kinesis-firehose.json)                                                                 | Kinesis Data Firehose | [data-transformation.html](https://docs.aws.amazon.com/firehose/latest/dev/data-transformation.html) |
| [kinesis-analytics-firehose-input-preprocessing.json](src/main/resources/schemas/kinesis-analytics-firehose-input-preprocessing.json)     | Kinesis Analytics Firehose preprocessing | [lambda-preprocessing.html](https://docs.aws.amazon.com/kinesisanalytics/latest/dev/lambda-preprocessing.html) |
| [kinesis-analytics-input-preprocessing-response.json](src/main/resources/schemas/kinesis-analytics-input-preprocessing-response.json)     | Kinesis Analytics preprocessing response | [lambda-preprocessing.html](https://docs.aws.amazon.com/kinesisanalytics/latest/dev/lambda-preprocessing.html) |
| [kinesis-analytics-output-delivery.json](src/main/resources/schemas/kinesis-analytics-output-delivery.json)                               | Kinesis Analytics output delivery | [lambda-preprocessing.html](https://docs.aws.amazon.com/kinesisanalytics/latest/dev/lambda-preprocessing.html) |
| [kinesis-analytics-output-delivery-response.json](src/main/resources/schemas/kinesis-analytics-output-delivery-response.json)             | Kinesis Analytics output delivery response | [lambda-preprocessing.html](https://docs.aws.amazon.com/kinesisanalytics/latest/dev/lambda-preprocessing.html) |
| [kinesis-analytics-streams-input-preprocessing.json](src/main/resources/schemas/kinesis-analytics-streams-input-preprocessing.json)       | Kinesis Analytics Streams preprocessing | [lambda-preprocessing.html](https://docs.aws.amazon.com/kinesisanalytics/latest/dev/lambda-preprocessing.html) |
| [kinesis-time-window.json](src/main/resources/schemas/kinesis-time-window.json)                                                           | Kinesis time window | [services-ddb-windows.html](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-windows.html) |
| [lambda-destination.json](src/main/resources/schemas/lambda-destination.json)                                                             | Lambda async invocation destination | [invocation-async.html](https://docs.aws.amazon.com/lambda/latest/dg/invocation-async.html) |
| [lex-event.json](src/main/resources/schemas/lex-event.json)                                                                               | Lex v1 event | [lambda.html](https://docs.aws.amazon.com/lexv2/latest/dg/lambda.html) |
| [lexv2-event.json](src/main/resources/schemas/lexv2-event.json)                                                                           | Lex v2 event | [lambda.html](https://docs.aws.amazon.com/lexv2/latest/dg/lambda.html) |
| [lexv2-response.json](src/main/resources/schemas/lexv2-response.json)                                                                     | Lex v2 response | [lambda.html](https://docs.aws.amazon.com/lexv2/latest/dg/lambda.html) |
| [rabbitmq.json](src/main/resources/schemas/rabbitmq.json)                                                                                 | RabbitMQ event | [with-mq.html](https://docs.aws.amazon.com/lambda/latest/dg/with-mq.html) |
| [s3.json](src/main/resources/schemas/s3.json)                                                                                             | S3 event notification | [with-s3.html](https://docs.aws.amazon.com/lambda/latest/dg/with-s3.html) |
| [s3-batch.json](src/main/resources/schemas/s3-batch.json)                                                                                 | S3 batch invocation | [services-s3-batch.html](https://docs.aws.amazon.com/lambda/latest/dg/services-s3-batch.html) |
| [s3-batch-response.json](src/main/resources/schemas/s3-batch-response.json)                                                               | S3 batch response | [services-s3-batch.html](https://docs.aws.amazon.com/lambda/latest/dg/services-s3-batch.html) |
| [s3-batch-v2.json](src/main/resources/schemas/s3-batch-v2.json)                                                                           | S3 batch v2 invocation | [services-s3-batch.html](https://docs.aws.amazon.com/lambda/latest/dg/services-s3-batch.html) |
| [s3-object-lambda.json](src/main/resources/schemas/s3-object-lambda.json)                                                                 | S3 Object Lambda | [olap-writing-lambda.html](https://docs.aws.amazon.com/AmazonS3/latest/userguide/olap-writing-lambda.html) |
| [scheduled.json](src/main/resources/schemas/scheduled.json)                                                                               | EventBridge Scheduler / CloudWatch Events | [with-eventbridge-scheduler.html](https://docs.aws.amazon.com/lambda/latest/dg/with-eventbridge-scheduler.html) |
| [secrets-manager-rotation.json](src/main/resources/schemas/secrets-manager-rotation.json)                                                 | Secrets Manager rotation | [rotate-secrets_lambda.html](https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotate-secrets_lambda.html) |
| [ses.json](src/main/resources/schemas/ses.json)                                                                                           | SES event notification | [receiving-email-action-lambda.html](https://docs.aws.amazon.com/ses/latest/dg/receiving-email-action-lambda.html) |
| [sns.json](src/main/resources/schemas/sns.json)                                                                                           | SNS event | [with-sns.html](https://docs.aws.amazon.com/lambda/latest/dg/with-sns.html) |
| [sqs.json](src/main/resources/schemas/sqs.json)                                                                                           | SQS event | [with-sqs.html](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html) |
| [time-window-event-response.json](src/main/resources/schemas/time-window-event-response.json)                                             | Time window response (DynamoDB/Kinesis) | [services-ddb-windows.html](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-windows.html) |
| [vpc-lattice-v2-request.json](src/main/resources/schemas/vpc-lattice-v2-request.json)                                                     | VPC Lattice request | [lambda-functions.html](https://docs.aws.amazon.com/vpc-lattice/latest/ug/lambda-functions.html) |

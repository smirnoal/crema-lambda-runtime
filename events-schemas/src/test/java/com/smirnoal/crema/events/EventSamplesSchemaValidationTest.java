package com.smirnoal.crema.events;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventSamplesSchemaValidationTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', useHeadersInDisplayName = false, textBlock = """
            events/activemq-event-sample.json|schemas/activemq.json
            events/alb-request-sample.json|schemas/alb-request.json
            events/alb-response-sample.json|schemas/alb-response.json
            events/apigw-custom-authorizer-sample.json|schemas/apigw-custom-authorizer.json
            events/apigw-httpapi-sample.json|schemas/apigw-httpapi-request.json
            events/apigw-rest-sample.json|schemas/apigw-rest-request.json
            events/apigw-v2-custom-authorizer-sample.json|schemas/apigw-v2-custom-authorizer.json
            events/apigw-custom-authorizer-response-sample.json|schemas/apigw-custom-authorizer-response.json
            events/apigw-v2-custom-authorizer-response-sample.json|schemas/apigw-v2-custom-authorizer-response.json
            events/apigw-v2-custom-authorizer-response-iam-sample.json|schemas/apigw-v2-custom-authorizer-response.json
            events/apigw-v2-websocket-event-sample.json|schemas/apigw-v2-websocket-event.json
            events/apigw-v2-websocket-response-sample.json|schemas/apigw-v2-websocket-response.json
            events/appsync-authorizer-event-sample.json|schemas/appsync-authorizer-event.json
            events/appsync-authorizer-response-sample.json|schemas/appsync-authorizer-response.json
            events/cloudformation-custom-resource-event-sample.json|schemas/cloudformation-custom-resource.json
            events/cloudfront-event-sample.json|schemas/cloudfront.json
            events/cloudwatch-composite-alarm-event-sample.json|schemas/cloudwatch-composite-alarm.json
            events/cloudwatch-metric-alarm-event-sample.json|schemas/cloudwatch-metric-alarm.json
            events/cloudwatchlogs-sample.json|schemas/cloudwatch-logs.json
            events/codecommit-event-sample.json|schemas/codecommit.json
            events/codepipeline-event-sample.json|schemas/codepipeline.json
            events/config-event-sample.json|schemas/config.json
            events/cognito-event-sample.json|schemas/cognito-event.json
            events/cognito-user-pool-create-auth-challenge-sample.json|schemas/cognito-user-pool-create-auth-challenge.json
            events/cognito-user-pool-custom-message-sample.json|schemas/cognito-user-pool-custom-message.json
            events/cognito-user-pool-custom-sms-sender-sample.json|schemas/cognito-user-pool-custom-sms-sender.json
            events/cognito-user-pool-define-auth-challenge-sample.json|schemas/cognito-user-pool-define-auth-challenge.json
            events/cognito-user-pool-inbound-federation-sample.json|schemas/cognito-user-pool-inbound-federation.json
            events/cognito-user-pool-migrate-user-sample.json|schemas/cognito-user-pool-migrate-user.json
            events/cognito-user-pool-post-authentication-sample.json|schemas/cognito-user-pool-post-authentication.json
            events/cognito-user-pool-post-confirmation-sample.json|schemas/cognito-user-pool-post-confirmation.json
            events/cognito-user-pool-pre-authentication-sample.json|schemas/cognito-user-pool-pre-authentication.json
            events/cognito-user-pool-pre-signup-sample.json|schemas/cognito-user-pool-pre-signup.json
            events/cognito-user-pool-pre-token-generation-sample.json|schemas/cognito-user-pool-pre-token-generation.json
            events/cognito-user-pool-pre-token-generation-v2-sample.json|schemas/cognito-user-pool-pre-token-generation-v2.json
            events/cognito-user-pool-verify-auth-challenge-response-sample.json|schemas/cognito-user-pool-verify-auth-challenge-response.json
            events/connect-event-sample.json|schemas/connect-event.json
            events/dynamodb-streams-sample.json|schemas/dynamodb-streams.json
            events/dynamodb-time-window-event-sample.json|schemas/dynamodb-time-window.json
            events/eventbridge-sample.json|schemas/eventbridge.json
            events/iot-button-event-sample.json|schemas/iot-button.json
            events/iot-custom-authorizer-event-sample.json|schemas/iot-custom-authorizer-event.json
            events/kafka-event-sample.json|schemas/kafka.json
            events/kinesis-sample.json|schemas/kinesis.json
            events/kinesis-firehose-event-sample.json|schemas/kinesis-firehose.json
            events/kinesis-analytics-firehose-input-preprocessing-event-sample.json|schemas/kinesis-analytics-firehose-input-preprocessing.json
            events/kinesis-analytics-input-preprocessing-response-event-sample.json|schemas/kinesis-analytics-input-preprocessing-response.json
            events/kinesis-analytics-output-delivery-event-sample.json|schemas/kinesis-analytics-output-delivery.json
            events/kinesis-analytics-output-delivery-response-event-sample.json|schemas/kinesis-analytics-output-delivery-response.json
            events/kinesis-analytics-streams-input-preprocessing-event-sample.json|schemas/kinesis-analytics-streams-input-preprocessing.json
            events/kinesis-time-window-event-sample.json|schemas/kinesis-time-window.json
            events/lambda-destination-event-sample.json|schemas/lambda-destination.json
            events/lex-event-sample.json|schemas/lex-event.json
            events/lexv2-event-sample.json|schemas/lexv2-event.json
            events/lexv2-response-sample.json|schemas/lexv2-response.json
            events/rabbitmq-event-sample.json|schemas/rabbitmq.json
            events/s3-sample.json|schemas/s3.json
            events/s3-batch-event-sample.json|schemas/s3-batch.json
            events/s3-batch-response-sample.json|schemas/s3-batch-response.json
            events/s3-batch-v2-event-sample.json|schemas/s3-batch-v2.json
            events/s3-object-lambda-event-sample.json|schemas/s3-object-lambda.json
            events/scheduled-sample.json|schemas/scheduled.json
            events/scheduled-v2-sample.json|schemas/scheduled.json
            events/secrets-manager-rotation-event-sample.json|schemas/secrets-manager-rotation.json
            events/ses-event-sample.json|schemas/ses.json
            events/sns-sample.json|schemas/sns.json
            events/sqs-sample.json|schemas/sqs.json
            events/time-window-event-response-sample.json|schemas/time-window-event-response.json
            events/vpc-lattice-v2-request-event-sample.json|schemas/vpc-lattice-v2-request.json
            """)
    void sampleConformsToSchema(String samplePath, String schemaPath) throws IOException {
        String sampleJson = loadResource(samplePath);
        String schemaJson = loadResource(schemaPath);

        JSONObject schemaObject = new JSONObject(new JSONTokener(schemaJson));
        JSONObject sampleObject = new JSONObject(new JSONTokener(sampleJson));

        Schema schema = SchemaLoader.builder().schemaJson(schemaObject).build().load().build();
        assertDoesNotThrow(() -> schema.validate(sampleObject));
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream is = EventSamplesSchemaValidationTest.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

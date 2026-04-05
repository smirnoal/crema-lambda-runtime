package com.smirnoal.crema;

import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvocationContextTest {

    @Test
    void getAwsRequestId_returnsRequestId() {
        InvocationRequest request = InvocationRequest.builder()
                .withId("test-request-id-123")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        assertEquals("test-request-id-123", ctx.getAwsRequestId());
    }

    @Test
    void getRemainingTimeInMillis_returnsPositiveWhenBeforeDeadline() {
        long deadline = System.currentTimeMillis() + 5_000;
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(deadline)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        int remaining = ctx.getRemainingTimeInMillis();
        assertEquals(5_000, remaining, 1_000); // allow 1s tolerance
    }

    @Test
    void getRemainingTimeInMillis_returnsZeroWhenPastDeadline() {
        long deadline = System.currentTimeMillis() - 1_000;
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(deadline)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        assertEquals(0, ctx.getRemainingTimeInMillis());
    }

    @Test
    void getInvokedFunctionArn_returnsArn() {
        String arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .withInvokedFunctionArn(arn)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        assertEquals(arn, ctx.getInvokedFunctionArn());
    }

    @Test
    void getClientContextBase64_returnsRawString() {
        String clientCtx = "base64-encoded-client-context";
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .withClientContext(clientCtx)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        assertEquals(clientCtx, ctx.getClientContextBase64());
    }

    @Test
    void getCognitoIdentityBase64_returnsRawString() {
        String cognito = "base64-encoded-cognito-identity";
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .withCognitoIdentity(cognito)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        assertEquals(cognito, ctx.getCognitoIdentityBase64());
    }

    @Test
    void getMemoryLimitInMB_returnsZeroWhenEnvUnset() {
        InvocationRequest request = InvocationRequest.builder()
                .withId("id")
                .withContent(new byte[0])
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .build();
        InvocationContext ctx = new InvocationContext(request);

        // Environment.AWS_LAMBDA_FUNCTION_MEMORY_SIZE is typically unset in unit tests
        int memory = ctx.getMemoryLimitInMB();
        assertEquals(0, memory);
    }
}

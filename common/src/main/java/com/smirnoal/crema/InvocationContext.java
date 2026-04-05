package com.smirnoal.crema;

import com.smirnoal.crema.rapid.client.dto.InvocationRequest;

import static com.smirnoal.crema.Lambda.Environment;

/**
 * Facade over invocation metadata, aligned with AWS Lambda Context semantics.
 * Provides typed accessors for request ID, remaining time, function ARN, and
 * environment-derived values.
 *
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/java-context.html">AWS Lambda context object</a>
 */
public final class InvocationContext {

    private final InvocationRequest request;

    public InvocationContext(InvocationRequest request) {
        this.request = request;
    }

    /**
     * Returns the AWS request ID associated with the invocation.
     */
    public String getAwsRequestId() {
        return request.id();
    }

    /**
     * Returns the time remaining for this execution in milliseconds.
     */
    public int getRemainingTimeInMillis() {
        long now = System.currentTimeMillis();
        long delta = request.deadlineTimeInMs() - now;
        return delta > 0 ? (int) delta : 0;
    }

    /**
     * Returns the ARN used to invoke the function.
     */
    public String getInvokedFunctionArn() {
        return request.invokedFunctionArn();
    }

    /**
     * Returns the raw client context string (base64-encoded JSON when from mobile SDK).
     */
    public String getClientContextBase64() {
        return request.clientContext();
    }

    /**
     * Returns the raw Cognito identity string (base64-encoded JSON when from mobile SDK).
     */
    public String getCognitoIdentityBase64() {
        return request.cognitoIdentity();
    }

    /**
     * Returns the name of the function being executed.
     */
    public String getFunctionName() {
        return Environment.AWS_LAMBDA_FUNCTION_NAME;
    }

    /**
     * Returns the version of the function being executed.
     */
    public String getFunctionVersion() {
        return Environment.AWS_LAMBDA_FUNCTION_VERSION;
    }

    /**
     * Returns the CloudWatch log group for this function.
     */
    public String getLogGroupName() {
        return Environment.AWS_LAMBDA_LOG_GROUP_NAME;
    }

    /**
     * Returns the CloudWatch log stream for this function instance.
     */
    public String getLogStreamName() {
        return Environment.AWS_LAMBDA_LOG_STREAM_NAME;
    }

    /**
     * Returns the memory size configured for the Lambda function in MB.
     */
    public int getMemoryLimitInMB() {
        String mem = Environment.AWS_LAMBDA_FUNCTION_MEMORY_SIZE;
        if (mem == null || mem.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(mem);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

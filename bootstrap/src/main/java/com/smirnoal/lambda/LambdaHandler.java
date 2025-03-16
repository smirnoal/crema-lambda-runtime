package com.smirnoal.lambda;

public interface LambdaHandler<InputType, OutputType> {
    OutputType handle(InputType event);

    byte[] toBytes(OutputType object);

    InputType toInputType(byte[] bytes);
}

package com.smirnoal.lambda;

import java.util.function.Function;

public interface LambdaHandler<InputType, OutputType> {
    OutputType handle(InputType event);

    byte[] toBytes(OutputType object);

    InputType toInputType(byte[] bytes);

}

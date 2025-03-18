package com.smirnoal.lambda.serde;

import java.util.function.Function;

public interface LambdaSerde<T, R> {
    /**
     * Function to create input object from byte array
     * @return object of input type
     */
    Function<byte[], T> inputDeserializer();

    /**
     * Function to create byte array from an output object
     * @return byte array
     */
    Function<R, byte[]> outputSerializer();
}

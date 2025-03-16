package com.smirnoal.lambda;

import java.util.function.Function;

public interface LambdaSerDe<T, R> {
    /**
     * Function to create input object from byte array
     * @return object of input type
     */
    Function<byte[], T> getInputDeserializer();

    /**
     * Function to create byte array from an output object
     * @return byte array
     */
    Function<R, byte[]> getOutputSerializer();
}

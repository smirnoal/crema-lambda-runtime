package com.smirnoal.crema.serde;

import java.util.function.Function;

public interface LambdaSerde<T, R> {
    /**
     * Function to create an input object from a byte array.
     * @return an object of the input type
     */
    Function<byte[], T> inputDeserializer();

    /**
     * Function to create a byte array from an output object.
     * @return a byte array
     */
    Function<R, byte[]> outputSerializer();
}

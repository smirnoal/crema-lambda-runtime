package com.smirnoal.crema.serde;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LambdaSerdeTest {
    
    @Test
    void testStringSerde() {
        // Test the StringSerde implementation
        LambdaSerde<String, String> stringSerde = new StringSerde();
        
        String testInput = "hello world";
        byte[] serialized = stringSerde.outputSerializer().apply(testInput);
        String deserialized = stringSerde.inputDeserializer().apply(serialized);
        
        assertEquals(testInput, deserialized, "String should be serialized and deserialized correctly");
    }
    
    @Test
    void testEmptyStringSerde() {
        // Test with empty string
        LambdaSerde<String, String> stringSerde = new StringSerde();
        
        String testInput = "";
        byte[] serialized = stringSerde.outputSerializer().apply(testInput);
        String deserialized = stringSerde.inputDeserializer().apply(serialized);
        
        assertEquals(testInput, deserialized, "Empty string should be handled correctly");
    }
}

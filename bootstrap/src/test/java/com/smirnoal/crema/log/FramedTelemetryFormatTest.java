package com.smirnoal.crema.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.smirnoal.crema.LambdaTestUtils.withFormattingSink;
import static org.junit.jupiter.api.Assertions.*;

class FramedTelemetryFormatTest {

    @TempDir
    Path tempDir;

    private Path tempFile;
    private FramedTelemetryLogSink sink;
    private FileDescriptor testFd;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile(tempDir, "telemetry-test", ".log");
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile());
        testFd = fileOutputStream.getFD();
        sink = new FramedTelemetryLogSink(testFd);
    }

    @Test
    void testFramedTelemetryFormat() throws IOException {
        String testMessage = "Test log message";
        byte[] messageBytes = testMessage.getBytes();
        
        sink.log(messageBytes);
        
        byte[] output = Files.readAllBytes(tempFile);
        
        // Verify frame structure
        assertTrue(output.length >= 16, "Output should be at least 16 bytes (header)");
        
        ByteBuffer buffer = ByteBuffer.wrap(output).order(ByteOrder.BIG_ENDIAN);
        
        // Read frame type (4 bytes)
        int frameType = buffer.getInt();
        assertEquals(0xa55a0003, frameType, "Frame type should be 0xa55a0003");
        
        // Read message length (4 bytes)
        int messageLength = buffer.getInt();
        assertEquals(messageBytes.length, messageLength, "Message length should match");
        
        // Read timestamp (8 bytes)
        long timestamp = buffer.getLong();
        assertTrue(timestamp > 0, "Timestamp should be positive");
        
        // Read message
        byte[] actualMessage = new byte[messageLength];
        buffer.get(actualMessage);
        assertArrayEquals(messageBytes, actualMessage, "Message content should match");
    }

    @Test
    void testMultipleMessages() throws IOException {
        String message1 = "First message";
        String message2 = "Second message";
        
        sink.log(message1.getBytes());
        sink.log(message2.getBytes());
        
        byte[] output = Files.readAllBytes(tempFile);
        
        // Should have two complete frames
        assertTrue(output.length >= 32, "Should have at least 32 bytes for two frames");
        
        ByteBuffer buffer = ByteBuffer.wrap(output).order(ByteOrder.BIG_ENDIAN);
        
        // Verify first frame
        int frameType1 = buffer.getInt();
        assertEquals(0xa55a0003, frameType1);
        int length1 = buffer.getInt();
        assertEquals(message1.getBytes().length, length1);
        buffer.getLong(); // skip timestamp
        byte[] actualMessage1 = new byte[length1];
        buffer.get(actualMessage1);
        assertArrayEquals(message1.getBytes(), actualMessage1);
        
        // Verify second frame
        int frameType2 = buffer.getInt();
        assertEquals(0xa55a0003, frameType2);
        int length2 = buffer.getInt();
        assertEquals(message2.getBytes().length, length2);
        buffer.getLong(); // skip timestamp
        byte[] actualMessage2 = new byte[length2];
        buffer.get(actualMessage2);
        assertArrayEquals(message2.getBytes(), actualMessage2);
    }

    @Test
    void testEmptyMessage() throws IOException {
        byte[] emptyMessage = new byte[0];
        sink.log(emptyMessage);
        
        byte[] output = Files.readAllBytes(tempFile);
        assertEquals(16, output.length, "Empty message should still have 16-byte header");
        
        ByteBuffer buffer = ByteBuffer.wrap(output).order(ByteOrder.BIG_ENDIAN);
        int frameType = buffer.getInt();
        assertEquals(0xa55a0003, frameType);
        int messageLength = buffer.getInt();
        assertEquals(0, messageLength, "Message length should be 0");
        buffer.getLong(); // skip timestamp
    }

    @Test
    void testPrintStreamProducesOneFramePerPrintln() throws Exception {
        Path printStreamFile = Files.createTempFile(tempDir, "telemetry-ps-test", ".log");
        withFormattingSink(printStreamFile, sink -> {
            try (FramedTelemetryPrintStream ps = new FramedTelemetryPrintStream(sink)) {
                ps.println("first line");
                ps.println("second line");
            }
        });

        byte[] output = Files.readAllBytes(printStreamFile);
        ByteBuffer buffer = ByteBuffer.wrap(output).order(ByteOrder.BIG_ENDIAN);

        // First frame: timestamp\trequestId\tUNDEFINED\tfirst line\n
        assertEquals(0xa55a0003, buffer.getInt());
        int len1 = buffer.getInt();
        buffer.getLong(); // timestamp
        byte[] msg1 = new byte[len1];
        buffer.get(msg1);
        String s1 = new String(msg1);
        assertTrue(s1.endsWith("\tUNDEFINED\tfirst line\n"), "Expected ...UNDEFINED\tfirst line\\n, got: " + s1);
        assertTrue(s1.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z\t.*\tUNDEFINED\tfirst line\n"), s1);

        // Second frame: timestamp\trequestId\tUNDEFINED\tsecond line\n
        assertEquals(0xa55a0003, buffer.getInt());
        int len2 = buffer.getInt();
        buffer.getLong(); // timestamp
        byte[] msg2 = new byte[len2];
        buffer.get(msg2);
        String s2 = new String(msg2);
        assertTrue(s2.endsWith("\tUNDEFINED\tsecond line\n"), "Expected ...UNDEFINED\tsecond line\\n, got: " + s2);

        assertFalse(buffer.hasRemaining(), "No extra bytes beyond the two frames");
    }
} 
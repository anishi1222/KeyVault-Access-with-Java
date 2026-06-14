package kvsdk4java2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void shouldConvertTextToUtf16Bytes() {
        byte[] expected = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_16);

        assertArrayEquals(expected, App.toUtf16Bytes("hello"));
    }

    @Test
    void shouldConvertUtf16BytesToText() {
        byte[] utf16Bytes = "日本語".getBytes(java.nio.charset.StandardCharsets.UTF_16);

        assertEquals("日本語", App.fromUtf16Bytes(utf16Bytes));
    }

    @Test
    void shouldRoundTripUtf16Conversion() {
        String value = "This is a test 🔐";

        assertEquals(value, App.fromUtf16Bytes(App.toUtf16Bytes(value)));
    }
}

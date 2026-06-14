package kvsdk4java2;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTest {

    @Test
    void shouldConvertTextToUtf16Bytes() {
        byte[] expected = "hello".getBytes(StandardCharsets.UTF_16);

        assertArrayEquals(expected, App.toUtf16Bytes("hello"));
    }

    @Test
    void shouldConvertUtf16BytesToText() {
        byte[] utf16Bytes = "日本語".getBytes(StandardCharsets.UTF_16);

        assertEquals("日本語", App.fromUtf16Bytes(utf16Bytes));
    }

    @Test
    void shouldRoundTripUtf16Conversion() {
        String value = "This is a test 🔐";

        assertEquals(value, App.fromUtf16Bytes(App.toUtf16Bytes(value)));
    }

    @Test
    void shouldRoundTripEmptyStringUtf16Conversion() {
        assertEquals("", App.fromUtf16Bytes(App.toUtf16Bytes("")));
    }

    @Test
    void shouldThrowWhenRequiredEnvVarIsMissing() {
        String variableName = "UNIT_TEST_MISSING_ENV_" + UUID.randomUUID();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> App.requireEnv(variableName));

        assertEquals(variableName + " environment variable is required.", exception.getMessage());
    }

    @Test
    void shouldReturnRequiredEnvVarValueWhenPresent() {
        String variableName = "PATH";
        String expected = System.getenv(variableName);

        assumeTrue(expected != null && !expected.isBlank(), "PATH must be available for this test.");

        assertEquals(expected, App.requireEnv(variableName));
    }

    @Test
    void shouldThrowWhenConvertingNullTextToUtf16Bytes() {
        assertThrows(NullPointerException.class, () -> App.toUtf16Bytes(null));
    }

    @Test
    void shouldThrowWhenConvertingNullBytesFromUtf16() {
        assertThrows(NullPointerException.class, () -> App.fromUtf16Bytes(null));
    }

    @Test
    void shouldFailFastInMainWhenAzureConfigurationIsIncomplete() {
        boolean hasMissingRequiredEnv = Stream.of(
                App.AZURE_CLIENT_ID_ENV,
                App.AZURE_CLIENT_SECRET_ENV,
                App.AZURE_TENANT_ID_ENV,
                App.KEY_IDENTIFIER_ENV)
                .map(System::getenv)
                .anyMatch(value -> value == null || value.isBlank());

        assumeTrue(hasMissingRequiredEnv, "This test requires at least one missing AZURE_* env var.");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> App.main(new String[0]));

        assertTrue(exception.getMessage().endsWith("environment variable is required."));
    }

    @Test
    void shouldRunHappyPathWithoutNetworkUsingInjectedProvider() {
        Map<String, String> env = Map.of(
                App.AZURE_CLIENT_ID_ENV, "test-client-id",
                App.AZURE_CLIENT_SECRET_ENV, "test-client-secret",
                App.AZURE_TENANT_ID_ENV, "test-tenant-id",
                App.KEY_IDENTIFIER_ENV, "https://example.vault.azure.net/keys/test-key/test-version");

        byte[] plainText = App.toUtf16Bytes(App.TEXT_TO_ENCRYPT);
        byte[] cipherText = App.toUtf16Bytes("cipher-text");

        App.CryptoProvider cryptoProvider = mock(App.CryptoProvider.class);
        when(cryptoProvider.encrypt(any(byte[].class))).thenReturn(cipherText);
        when(cryptoProvider.decrypt(cipherText)).thenReturn(plainText);
        when(cryptoProvider.encryptAsync(any(byte[].class))).thenReturn(cipherText);
        when(cryptoProvider.decryptAsync(cipherText)).thenReturn(plainText);

        App.CryptoProviderFactory factory = (clientId, clientSecret, tenantId, keyIdentifier) -> {
            assertEquals("test-client-id", clientId);
            assertEquals("test-client-secret", clientSecret);
            assertEquals("test-tenant-id", tenantId);
            assertEquals("https://example.vault.azure.net/keys/test-key/test-version", keyIdentifier);
            return cryptoProvider;
        };

        assertDoesNotThrow(() -> App.run(env::get, factory));

        verify(cryptoProvider, times(1)).encrypt(any(byte[].class));
        verify(cryptoProvider, times(1)).decrypt(cipherText);
        verify(cryptoProvider, times(1)).encryptAsync(any(byte[].class));
        verify(cryptoProvider, times(1)).decryptAsync(cipherText);
    }

    @Test
    void shouldThrowWhenAsyncEncryptionReturnsNull() {
        Map<String, String> env = Map.of(
                App.AZURE_CLIENT_ID_ENV, "test-client-id",
                App.AZURE_CLIENT_SECRET_ENV, "test-client-secret",
                App.AZURE_TENANT_ID_ENV, "test-tenant-id",
                App.KEY_IDENTIFIER_ENV, "https://example.vault.azure.net/keys/test-key/test-version");

        byte[] plainText = App.toUtf16Bytes(App.TEXT_TO_ENCRYPT);
        byte[] cipherText = App.toUtf16Bytes("cipher-text");

        App.CryptoProvider cryptoProvider = mock(App.CryptoProvider.class);
        when(cryptoProvider.encrypt(any(byte[].class))).thenReturn(cipherText);
        when(cryptoProvider.decrypt(cipherText)).thenReturn(plainText);
        when(cryptoProvider.encryptAsync(any(byte[].class))).thenReturn(null);

        App.CryptoProviderFactory factory = (clientId, clientSecret, tenantId, keyIdentifier) -> cryptoProvider;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> App.run(env::get, factory));

        assertEquals("Async encryption returned null result.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAsyncDecryptionReturnsNull() {
        Map<String, String> env = Map.of(
                App.AZURE_CLIENT_ID_ENV, "test-client-id",
                App.AZURE_CLIENT_SECRET_ENV, "test-client-secret",
                App.AZURE_TENANT_ID_ENV, "test-tenant-id",
                App.KEY_IDENTIFIER_ENV, "https://example.vault.azure.net/keys/test-key/test-version");

        byte[] plainText = App.toUtf16Bytes(App.TEXT_TO_ENCRYPT);
        byte[] cipherText = App.toUtf16Bytes("cipher-text");

        App.CryptoProvider cryptoProvider = mock(App.CryptoProvider.class);
        when(cryptoProvider.encrypt(any(byte[].class))).thenReturn(cipherText);
        when(cryptoProvider.decrypt(cipherText)).thenReturn(plainText);
        when(cryptoProvider.encryptAsync(any(byte[].class))).thenReturn(cipherText);
        when(cryptoProvider.decryptAsync(cipherText)).thenReturn(null);

        App.CryptoProviderFactory factory = (clientId, clientSecret, tenantId, keyIdentifier) -> cryptoProvider;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> App.run(env::get, factory));

        assertEquals("Async decryption returned null result.", exception.getMessage());
    }
}

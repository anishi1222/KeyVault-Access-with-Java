package kvsdk4java2;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link App}.
 *
 * <p>{@code App.main} constructs Azure Key Vault clients internally and performs encrypt/decrypt
 * round-trips. To exercise that code path without any network/credential dependency, all Azure SDK
 * builders and clients are replaced with Mockito mocks via {@code mockConstruction}. No production
 * source is modified.
 */
class AppTest {

    @Test
    void mainRunsEncryptDecryptRoundTripWithMockedKeyVaultClients() {
        // Arrange: canned crypto results returned by the mocked clients.
        byte[] cipher = "cipher-bytes".getBytes(StandardCharsets.UTF_16);
        byte[] plain = "This is a test".getBytes(StandardCharsets.UTF_16);

        EncryptResult encryptResult = mock(EncryptResult.class);
        when(encryptResult.getCipherText()).thenReturn(cipher);
        DecryptResult decryptResult = mock(DecryptResult.class);
        when(decryptResult.getPlainText()).thenReturn(plain);

        CryptographyClient syncClient = mock(CryptographyClient.class);
        when(syncClient.encrypt(any(EncryptionAlgorithm.class), any(byte[].class))).thenReturn(encryptResult);
        when(syncClient.decrypt(any(EncryptionAlgorithm.class), any(byte[].class))).thenReturn(decryptResult);

        CryptographyAsyncClient asyncClient = mock(CryptographyAsyncClient.class);
        when(asyncClient.encrypt(any(EncryptionAlgorithm.class), any(byte[].class))).thenReturn(Mono.just(encryptResult));
        when(asyncClient.decrypt(any(EncryptionAlgorithm.class), any(byte[].class))).thenReturn(Mono.just(decryptResult));

        ClientSecretCredential credential = mock(ClientSecretCredential.class);

        try (MockedConstruction<ClientSecretCredentialBuilder> credentialBuilders = mockConstruction(
                ClientSecretCredentialBuilder.class,
                (builder, context) -> {
                    when(builder.clientId(any())).thenReturn(builder);
                    when(builder.clientSecret(any())).thenReturn(builder);
                    when(builder.tenantId(any())).thenReturn(builder);
                    when(builder.build()).thenReturn(credential);
                });
             MockedConstruction<CryptographyClientBuilder> cryptoBuilders = mockConstruction(
                CryptographyClientBuilder.class,
                (builder, context) -> {
                    when(builder.credential(any())).thenReturn(builder);
                    when(builder.keyIdentifier(any())).thenReturn(builder);
                    when(builder.buildClient()).thenReturn(syncClient);
                    when(builder.buildAsyncClient()).thenReturn(asyncClient);
                })) {

            // Act
            App.main(new String[]{});

            // Assert: one credential builder, two cryptography builders (sync + async) were created.
            assertEquals(1, credentialBuilders.constructed().size());
            assertEquals(2, cryptoBuilders.constructed().size());

            // The synchronous path encrypts then decrypts exactly once.
            verify(syncClient, times(1)).encrypt(any(EncryptionAlgorithm.class), any(byte[].class));
            verify(syncClient, times(1)).decrypt(any(EncryptionAlgorithm.class), any(byte[].class));

            // The asynchronous path encrypts then decrypts exactly once.
            verify(asyncClient, times(1)).encrypt(any(EncryptionAlgorithm.class), any(byte[].class));
            verify(asyncClient, times(1)).decrypt(any(EncryptionAlgorithm.class), any(byte[].class));
        }
    }
}

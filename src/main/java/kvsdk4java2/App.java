package kvsdk4java2;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;

public class App {

        private static final Logger LOG = Logger.getLogger(App.class.getName());

        static final String AZURE_CLIENT_ID_ENV = "AZURE_CLIENT_ID";
        static final String AZURE_CLIENT_SECRET_ENV = "AZURE_CLIENT_SECRET";
        static final String AZURE_TENANT_ID_ENV = "AZURE_TENANT_ID";
        static final String KEY_IDENTIFIER_ENV = "AZURE_KEY_IDENTIFIER";
        static final String TEXT_TO_ENCRYPT = "This is a test";

        @FunctionalInterface
        interface EnvReader {
                String get(String variableName);
        }

        interface CryptoProvider {
                byte[] encrypt(byte[] plainText);

                byte[] decrypt(byte[] cipherText);

                byte[] encryptAsync(byte[] plainText);

                byte[] decryptAsync(byte[] cipherText);
        }

        @FunctionalInterface
        interface CryptoProviderFactory {
                CryptoProvider create(String clientId, String clientSecret, String tenantId, String keyIdentifier);
        }

        static final CryptoProviderFactory DEFAULT_CRYPTO_PROVIDER_FACTORY =
                        (azureClientId, azureClientSecret, azureTenantId, keyIdentifier) -> {
                                ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                                                .clientId(azureClientId)
                                                .clientSecret(azureClientSecret)
                                                .tenantId(azureTenantId)
                                                .build();

                                CryptographyClient cryptographyClient = new CryptographyClientBuilder()
                                                .credential(clientSecretCredential)
                                                .keyIdentifier(keyIdentifier)
                                                .buildClient();

                                CryptographyAsyncClient cryptographyAsyncClient = new CryptographyClientBuilder()
                                                .credential(clientSecretCredential)
                                                .keyIdentifier(keyIdentifier)
                                                .buildAsyncClient();

                                return new CryptoProvider() {
                                        @Override
                                        public byte[] encrypt(byte[] plainText) {
                                                EncryptResult encryptResult = cryptographyClient.encrypt(EncryptionAlgorithm.RSA_OAEP,
                                                                plainText);
                                                return encryptResult.getCipherText();
                                        }

                                        @Override
                                        public byte[] decrypt(byte[] cipherText) {
                                                DecryptResult decryptResult = cryptographyClient.decrypt(EncryptionAlgorithm.RSA_OAEP,
                                                                cipherText);
                                                return decryptResult.getPlainText();
                                        }

                                        @Override
                                        public byte[] encryptAsync(byte[] plainText) {
                                                EncryptResult encryptResult = cryptographyAsyncClient
                                                                .encrypt(EncryptionAlgorithm.RSA_OAEP, plainText)
                                                                .block();
                                                if (encryptResult == null) {
                                                        return null;
                                                }
                                                return encryptResult.getCipherText();
                                        }

                                        @Override
                                        public byte[] decryptAsync(byte[] cipherText) {
                                                DecryptResult decryptResult = cryptographyAsyncClient
                                                                .decrypt(EncryptionAlgorithm.RSA_OAEP, cipherText)
                                                                .block();
                                                if (decryptResult == null) {
                                                        return null;
                                                }
                                                return decryptResult.getPlainText();
                                        }
                                };
                        };

        static String requireEnv(String variableName) {
                return requireEnv(System::getenv, variableName);
        }

        static String requireEnv(EnvReader envReader, String variableName) {
                String value = envReader.get(variableName);
                if (value == null || value.isBlank()) {
                        throw new IllegalStateException(variableName + " environment variable is required.");
                }
                return value;
        }

    static byte[] toUtf16Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_16);
    }

    static String fromUtf16Bytes(byte[] value) {
        return new String(value, StandardCharsets.UTF_16);
    }

    static void run(EnvReader envReader, CryptoProviderFactory cryptoProviderFactory) {

        // Read credentials and key identifier from environment variables.
        String azureClientId = requireEnv(envReader, AZURE_CLIENT_ID_ENV);
        String azureClientSecret = requireEnv(envReader, AZURE_CLIENT_SECRET_ENV);
        String azureTenantId = requireEnv(envReader, AZURE_TENANT_ID_ENV);
        String keyIdentifier = requireEnv(envReader, KEY_IDENTIFIER_ENV);

        CryptoProvider cryptoProvider = cryptoProviderFactory.create(
                azureClientId,
                azureClientSecret,
                azureTenantId,
                keyIdentifier);

        LOG.info(() -> "[textToEncryptLength]" + TEXT_TO_ENCRYPT.length());
        byte[] byteText = toUtf16Bytes(TEXT_TO_ENCRYPT);
        byte[] encryptedSync = cryptoProvider.encrypt(byteText);
        LOG.info(() -> "[EncryptedBytesLength]" + encryptedSync.length);

        byte[] decryptedSync = cryptoProvider.decrypt(encryptedSync);
        String decrypted1 = fromUtf16Bytes(decryptedSync);
        LOG.info(() -> "[SyncRoundTripMatch]" + String.valueOf(TEXT_TO_ENCRYPT.equals(decrypted1)));


        LOG.info("---Async---");
        LOG.info(() -> "[textToEncryptLength]" + TEXT_TO_ENCRYPT.length());
        byte[] encryptedAsync = cryptoProvider.encryptAsync(byteText);
        if (encryptedAsync == null) {
            throw new IllegalStateException("Async encryption returned null result.");
        }
        LOG.info(() -> "[EncryptedBytesLength]" + encryptedAsync.length);

        byte[] decryptedAsync = cryptoProvider.decryptAsync(encryptedAsync);
        if (decryptedAsync == null) {
            throw new IllegalStateException("Async decryption returned null result.");
        }
        String decrypted2 = fromUtf16Bytes(decryptedAsync);
        LOG.info(() -> "[AsyncRoundTripMatch]" + String.valueOf(TEXT_TO_ENCRYPT.equals(decrypted2)));
    }

    public static void main(String[] args) {
        run(System::getenv, DEFAULT_CRYPTO_PROVIDER_FACTORY);
    }
}

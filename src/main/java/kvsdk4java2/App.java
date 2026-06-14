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

    static final String AZURE_CLIENT_ID = "Client ID";
    static final String AZURE_CLIENT_SECRET = "Client Secret";
    static final String AZURE_TENANT_ID = "....";
    static final String KEY_IDENTIFIER = "https://{KeyContainer}.vault.azure.net/keys/{KeyName}/{KeyVersion}";
    static String textToEncrypt = "This is a test";

    static byte[] toUtf16Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_16);
    }

    static String fromUtf16Bytes(byte[] value) {
        return new String(value, StandardCharsets.UTF_16);
    }

    public static void main(String[] args) {

        // authenticate with client secret,
        Logger log = Logger.getLogger("App");
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(AZURE_CLIENT_ID)
                .clientSecret(AZURE_CLIENT_SECRET)
                .tenantId(AZURE_TENANT_ID)
                .build();

        CryptographyClient cryptographyClient = new CryptographyClientBuilder()
                .credential(clientSecretCredential)
                .keyIdentifier(KEY_IDENTIFIER)
                .buildClient();

        log.info("[textToEncrypt]" + textToEncrypt);
        byte[] byteText = toUtf16Bytes(textToEncrypt);
        EncryptResult encryptResult1 = cryptographyClient.encrypt(EncryptionAlgorithm.RSA_OAEP, byteText);
        log.info("[Encrypted]" + fromUtf16Bytes(encryptResult1.cipherText()));

        DecryptResult decryptResult1 = cryptographyClient.decrypt(EncryptionAlgorithm.RSA_OAEP, encryptResult1.cipherText());
        log.info("[Decrypted]" + textToEncrypt + "<===>" + fromUtf16Bytes(decryptResult1.plainText()));


        log.info("---Async---");
        CryptographyAsyncClient cryptographyAsyncClient = new CryptographyClientBuilder()
                .credential(clientSecretCredential)
                .keyIdentifier(KEY_IDENTIFIER)
                .buildAsyncClient();

        log.info("[textToEncrypt]" + textToEncrypt);
        EncryptResult encryptResult2 = cryptographyAsyncClient
                .encrypt(EncryptionAlgorithm.RSA_OAEP, byteText)
                .block();
        log.info("[Encrypted]" + fromUtf16Bytes(encryptResult2.cipherText()));

        DecryptResult decryptResult2 = cryptographyAsyncClient
                .decrypt(EncryptionAlgorithm.RSA_OAEP, encryptResult2.getCipherText())
                .block();
        log.info("[Decrypted]" + textToEncrypt + "<=======>" + fromUtf16Bytes(decryptResult2.plainText()));
    }
}

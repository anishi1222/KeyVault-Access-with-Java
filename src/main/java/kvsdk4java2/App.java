package kvsdk4java2;

import com.azure.identity.credential.ClientSecretCredential;
import com.azure.identity.credential.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class App {

    static final String AZURE_CLIENT_ID = "Client ID";
    static final String AZURE_CLIENT_SECRET = "Client Secret";
    static final String AZURE_TENANT_ID = "....";
    static final String KEY_IDENTIFIER = "https://{KeyContainer}.vault.azure.net/keys/{KeyName}/{KeyVersion}";
    static String textToEncrypt = "This is a test";

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
        byte[] byteText = textToEncrypt.getBytes(StandardCharsets.UTF_16);
        EncryptResult encryptResult1 = cryptographyClient.encrypt(EncryptionAlgorithm.RSA_OAEP, byteText);
        log.info("[Encrypted]" + new String(encryptResult1.cipherText(), StandardCharsets.UTF_16));

        DecryptResult decryptResult1 = cryptographyClient.decrypt(EncryptionAlgorithm.RSA_OAEP, encryptResult1.cipherText());
        log.info("[Decrypted]" + textToEncrypt + "<===>" + new String(decryptResult1.plainText(), StandardCharsets.UTF_16));


        log.info("---Async---");
        CryptographyAsyncClient cryptographyAsyncClient = new CryptographyClientBuilder()
                .credential(clientSecretCredential)
                .keyIdentifier(KEY_IDENTIFIER)
                .buildAsyncClient();

        log.info("[textToEncrypt]" + textToEncrypt);
        EncryptResult encryptResult2 = cryptographyAsyncClient
                .encrypt(EncryptionAlgorithm.RSA_OAEP, byteText)
                .block();
        log.info("[Encrypted]" + new String(encryptResult2.cipherText(), StandardCharsets.UTF_16));

        DecryptResult decryptResult2 = cryptographyAsyncClient
                .decrypt(EncryptionAlgorithm.RSA_OAEP, encryptResult2.cipherText())
                .block();
        log.info("[Decrypted]" + textToEncrypt + "<=======>" + new String(decryptResult2.plainText(), StandardCharsets.UTF_16));
    }
}

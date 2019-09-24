package sample.keyvaultclient;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;

import java.io.UnsupportedEncodingException;

public class App {

	static final String KEYVAULT_URL = "DNS Name for created Key container";
	static final String AZURE_CLIENT_ID = "Application Client ID, which was created in Azure Active Directory";
	static final String AZURE_CLIENT_SECRET = "Client Secret, which was created in Azure Active Directory";
	static final String KEY_IDENTIFIER = "https://....";
	static String textToEncrypt = "This is a test";

	public static void main(String... args) {
		KeyVaultCredentials credential = new ClientSecretKeyVaultCredential( AZURE_CLIENT_ID, AZURE_CLIENT_SECRET );
        	KeyVaultClient client = new KeyVaultClient(credential);
        	System.out.println("[textToEncrypt]" + textToEncrypt);
		
        	try {
            		byte[] byteText = textToEncrypt.getBytes("UTF-16");
			// In fact, async methods should be used for production use, but sync methods are used for explanation purpose.
            		KeyOperationResult encryptResult = client.encrypt(KEY_IDENTIFIER, JsonWebKeyEncryptionAlgorithm.RSA_OAEP_256, byteText);
            		System.out.println("[Encrypt] kid=" +encryptResult.kid() + " / " + new String(encryptResult.result(), "UTF-16"));

            		KeyOperationResult decryptResult = client.decrypt(KEY_IDENTIFIER, JsonWebKeyEncryptionAlgorithm.RSA_OAEP_256, encryptResult.result());
            		System.out.println("[Decrypt] kid=" + decryptResult.kid());

            		String answer = new String (decryptResult.result(), "UTF-16");
            		System.out.println("[Compare] " + textToEncrypt + " <===> " + answer);
        	} catch (UnsupportedEncodingException e) {
            		e.printStackTrace();
        	}
	}
}

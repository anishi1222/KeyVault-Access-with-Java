package sample.keyvaultclient;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;


public class App {

	static final String KEYVAULT_URL = "DNS Name for created Key container";
	static final String AZURE_CLIENT_ID = "Application Client ID, which was created in Azure Active Directory";
	static final String AZURE_CLIENT_SECRET = "Client Secret, which was created in Azure Active Directory";

	public static void main(String[] args) {
		if(args.length!=1) {
			System.out.println("java " + args[0] + "secret name");
			System.exit(1);
		}

		System.out.println("Reading data from Key Vault...");

		ClientSecretKeyVaultCredential credential = new ClientSecretKeyVaultCredential( AZURE_CLIENT_ID, AZURE_CLIENT_SECRET);		
		KeyVaultClient client = new KeyVaultClient(credential);
		
		SecretBundle secret = client.getSecret(KEYVAULT_URL, args[0]);
		System.out.println(secret.value());
	}
}

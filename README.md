# KeyVault-Access-with-Java (Reactor based)
Access Azure Key Vault through Java

## What is Azure Key Vault?
Refer to https://docs.microsoft.com/en-us/azure/key-vault/key-vault-whatis

## How to configure application to access key vault
- Application registration
  - Azure Active Directory > Manage > App registrations
  - Click "New application registration" and fill out the form (Redirect URL is not required).
  - Take a note of Application (client) ID after registration is completed. This Application (client) ID is required to connect to Key Vault.
- Password
  - Azure Active Directory > Manage > App registrations > Manage > "Client secrets" in Certificates & secrets section.
  - Enter description, duration, and value.
  - Click "Save", and value is changed. This changed value is "Client Secret", of which you have to take a note.
  
## How to configure Key Vault
- Choose Key Vault in All Service or search for "key vault" in "Create a resource" and create with information added.
- Take a note of DNS name. This URL is required to connect Key Vault.
- Access Policies
  - Click "Access Policies" in Settings, and click "Add new".
  - Click "OK" after the following items are set.
    - Select Principal : The application you registered.
    - Secret permissions : Select "Get" in SEcret Management Operations 
- Add secret to Key container
  - Click "Secrets" in Settings and click "Generate/Import"
  - Enter Name and Value. Name is the key when accessing key-value pair stored in Key Vault.
  - Make sure created secret is enabled.
  - Click "Create".

## Others
- Java Code
  - Please refer to [Azure Key Vault Key client library for Java](https://github.com/Azure/azure-sdk-for-java/tree/master/sdk/keyvault/azure-keyvault-keys#azure-key-vault-key-client-library-for-java)
  - This code sample used block() for description purpose. Do not use block() in production code. 

## Secure runtime configuration
Do not hard-code credentials in source code. Set required values as environment variables before running the app.

```bash
export AZURE_CLIENT_ID="<your-app-client-id>"
export AZURE_CLIENT_SECRET="<your-app-client-secret>"
export AZURE_TENANT_ID="<your-tenant-id>"
export AZURE_KEY_IDENTIFIER="https://<your-vault>.vault.azure.net/keys/<key-name>/<key-version>"
```

Then build and run with Maven:

```bash
mvn clean verify
java -jar target/keyvaultclient2.jar
```

## Unit tests (network-free)
The application entry flow is testable without Azure network access via dependency injection:

- `App.run(EnvReader, CryptoProviderFactory)` accepts injected environment and crypto provider implementations.
- Unit tests use mocked providers to validate sync and async encrypt/decrypt paths.
- Async guard behavior is also covered when provider results are null.

Run tests with:

```bash
mvn test
```

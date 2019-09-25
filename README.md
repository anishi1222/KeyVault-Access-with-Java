# KeyVault-Access-with-Java (Previous SDK)
Access Azure Key Vault through Java

## What is Azure Key Vault?
Refer to https://docs.microsoft.com/en-us/azure/key-vault/key-vault-whatis

## How to configure application to access key vault
- Application registration
  - Azure Active Directory > Manage > App registrations
  - Click "New application registration" and fill out the form (Correct Sign-on URL is not required).
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
  - You have to implement `KeyVaultCredentials#doAuthenticate()` since this method is abstract.

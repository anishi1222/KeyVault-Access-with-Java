# Azure Key Vault Keys — Cryptography Sample (Java, Reactor-based)

A minimal Java sample that performs **encrypt/decrypt round-trips** against an RSA key stored in **Azure Key Vault**, using the Azure SDK for Java. It demonstrates both the **synchronous** `CryptographyClient` and the **asynchronous (Project Reactor based)** `CryptographyAsyncClient`.

The application is intentionally small, but it is structured with a **dependency-injection seam** so the core flow can be unit-tested without any network or Azure access.

## Features
- RSA-OAEP encrypt/decrypt against a Key Vault key
- Synchronous and asynchronous (Project Reactor) execution paths
- Authentication with Microsoft Entra ID via `ClientSecretCredential`
- Fail-fast validation of required configuration
- Network-free unit tests through injected providers

## What it does
1. Reads credentials and the target key identifier from environment variables.
2. Builds Key Vault cryptography clients (synchronous and asynchronous).
3. Encodes the sample text `"This is a test"` as UTF-16 bytes.
4. Encrypts and then decrypts the bytes with RSA-OAEP — first synchronously, then asynchronously.
5. Logs the byte lengths and whether each round-trip matches the original text.

## Prerequisites
- Java 25 (JDK)
- Apache Maven 3.9+
- An Azure subscription
- An Azure Key Vault containing an **RSA key**
- A Microsoft Entra application (client ID + secret) granted cryptographic permissions on the key

## Azure setup
### 1. Register a Microsoft Entra application
- Go to **Microsoft Entra ID > App registrations > New registration**.
- After creation, note the **Application (client) ID** and **Directory (tenant) ID**.
- Open **Certificates & secrets > New client secret**, then copy the secret **Value** immediately (it is shown only once).

### 2. Create a Key Vault and an RSA key
- Create a Key Vault.
- Under **Keys**, generate or import an **RSA** key.
- Note its full key identifier, which has the form:
  `https://<vault-name>.vault.azure.net/keys/<key-name>/<key-version>`

### 3. Grant cryptographic permissions
This sample performs key cryptographic operations (encrypt/decrypt), so the application needs **key** permissions — not secret permissions:
- **RBAC vaults:** assign the **Key Vault Crypto User** role to the application at the vault (or key) scope.
- **Access policy vaults:** grant **Key permissions → Encrypt, Decrypt** to the application.

## Configuration
Provide configuration through environment variables — never hard-code secrets in source code.

| Variable | Description |
| --- | --- |
| `AZURE_CLIENT_ID` | Application (client) ID of the Entra application |
| `AZURE_CLIENT_SECRET` | Client secret value |
| `AZURE_TENANT_ID` | Directory (tenant) ID |
| `AZURE_KEY_IDENTIFIER` | Full key identifier URL (see above) |

```bash
export AZURE_CLIENT_ID="<application-client-id>"
export AZURE_CLIENT_SECRET="<client-secret-value>"
export AZURE_TENANT_ID="<directory-tenant-id>"
export AZURE_KEY_IDENTIFIER="https://<vault-name>.vault.azure.net/keys/<key-name>/<key-version>"
```

Any missing or blank value causes the application to fail fast with an `IllegalStateException`.

## Build and run
```bash
mvn clean verify
java -jar target/keyvaultclient2.jar
```

`mvn verify` runs the unit tests, builds `target/keyvaultclient2.jar`, and copies the runtime dependencies into `target/libs` (referenced from the JAR manifest classpath). The application entry point is `kvsdk4java2.App`.

Expected log highlights on success:

```
[SyncRoundTripMatch]true
---Async---
[AsyncRoundTripMatch]true
```

## How it works
The entry point delegates to a testable `run` method:

```java
public static void main(String[] args) {
    run(System::getenv, DEFAULT_CRYPTO_PROVIDER_FACTORY);
}
```

Three small abstractions decouple the flow from the environment and the Azure SDK:

- `EnvReader` — supplies configuration values (defaults to `System::getenv`).
- `CryptoProvider` — exposes `encrypt`/`decrypt` and their asynchronous counterparts.
- `CryptoProviderFactory` — builds a `CryptoProvider` from the resolved configuration.

`DEFAULT_CRYPTO_PROVIDER_FACTORY` wires up a `ClientSecretCredential` together with the Key Vault `CryptographyClient` and `CryptographyAsyncClient`. The asynchronous path uses Project Reactor and resolves results with `block()`, guarded by explicit null checks.

> Note: this sample calls `block()` for demonstration purposes only. Do not block reactive pipelines in production code.

## Unit tests (network-free)
Because the flow is injected with `EnvReader` and `CryptoProviderFactory`, both the happy path and the error paths are exercised with Mockito — no Azure access required.

Covered scenarios include:
- UTF-16 encode/decode round-trips (including multibyte text and emoji)
- Required environment-variable validation and fail-fast `main`
- The full encrypt/decrypt flow with a mocked provider
- Async guard behavior when provider results are `null`

```bash
mvn test
```

## Project structure
```
.
├── pom.xml
├── README.md
└── src
    ├── main/java/kvsdk4java2/App.java     # application entry point + DI seams
    └── test/java/kvsdk4java2/AppTest.java # network-free unit tests
```

## Tech stack
- Java 25, Apache Maven
- Azure SDK for Java (Key Vault Keys, Identity) managed via `azure-sdk-bom`
- Project Reactor (asynchronous client)
- JUnit Jupiter and Mockito (tests)

## Security notes
- Keep secrets out of source control; use environment variables or a secret store.
- Prefer managed identities or workload identity federation over client secrets where possible.
- Grant least-privilege cryptographic permissions, scoped to the specific key.

## References
- [Azure Key Vault overview](https://learn.microsoft.com/azure/key-vault/general/overview)
- [Azure Key Vault Keys client library for Java](https://learn.microsoft.com/java/api/overview/azure/security-keyvault-keys-readme)

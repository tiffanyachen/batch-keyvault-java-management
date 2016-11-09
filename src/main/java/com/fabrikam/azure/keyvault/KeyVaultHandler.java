package com.fabrikam.azure.keyvault;

import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.keyvault.models.KeyAttributes;
import com.microsoft.azure.keyvault.models.SecretAttributes;
import com.microsoft.azure.keyvault.requests.ImportKeyRequest;
import com.microsoft.azure.keyvault.requests.UpdateKeyRequest;
import com.microsoft.azure.keyvault.requests.UpdateSecretRequest;
import org.joda.time.DateTime;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.models.SecretItem;
import com.microsoft.azure.keyvault.models.KeyItem;
import java.io.*;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;

public class KeyVaultHandler {

    private KeyVaultClient vc;

    KeyVaultHandler (KeyVaultClient vc) {
        this.vc = vc;
    }

	/**
	* Creates a key of the specified type in the specified vault.
	*
	* @param vaultURL URL of the vault in Azure that the key will be created in
	* @param keyName Name that will be given to the key once created
	* @param jsonWebKeyType Type of key to be created:
	*            For valid key types, see JsonWebKeyType. 
    *            Supported JsonWebKey key types (kty) for Elliptic Curve, RSA, HSM, Octet. 
    *            Possible values include: 'EC', 'RSA', 'RSA-HSM', 'oct'
	*/

    public void createKey(String vaultURL, String keyName, JsonWebKeyType jsonWebKeyType) throws IOException {
        CreateKeyRequest crk = new CreateKeyRequest.Builder(vaultURL, keyName, jsonWebKeyType).build();
		vc.createKey(crk);
    }

	/**
	* Creates a secret with the specified value in the specified vault.
	*
	* @param vaultURL URL of the vault in Azure that the secret will be created in
	* @param secretName Name that will be given to the secret once created
	* @param secretValue Value of the secret that will be created
	*/

    public void createSecret(String vaultURL, String secretName, String secretValue) throws IOException {
        SetSecretRequest ssr = new SetSecretRequest.Builder(vaultURL, secretName, secretValue).build();
		vc.setSecret(ssr);

    }

	/**
	* Imports a key into the specified vault.
	*
	* @param vaultURL URL of the vault in Azure that the key will be created in
	* @param keyName Name that will be given to the key once created
	* @param jWebKey The JsonWebKey to be imported
	*/

    public void importKey(String vaultURL, String keyName, JsonWebKey jWebKey)  throws IOException{
		ImportKeyRequest ikr = new ImportKeyRequest.Builder(vaultURL, keyName, jWebKey).build();
		vc.importKey(ikr);
    }

	/**
	* Updates a key given a vault URL and key name. 
	*
	* @param vaultURL URL of the vault in Azure that the key exists in
	* @param keyName Name of the key that is to be updated
	*/

    public void updateKey(String vaultURL, String keyName) throws IOException {
        //KeyAttributes item to hold the changes we want to update, in this case: Expiration date to 12/25/2017
		KeyAttributes keyA = new KeyAttributes();
		keyA.withExpires(new DateTime(2017, 12, 25, 0, 0));

		//Create UpdateKeyRequest for the updateKey method, and call updateKey
		UpdateKeyRequest ukr = new UpdateKeyRequest.Builder(vaultURL, keyName).withAttributes(keyA).build();
		vc.updateKey(ukr);

    }

	/**
	* Updates a key given a vault URL and key name. 
	*
	* @param vaultURL URL of the vault in Azure that the secret exists in
	* @param keyName Name of the secret that is to be updated
	*/

    public void updateSecret(String vaultURL, String secretName) throws IOException {
        //SecretAttributes item to hold the changes we want to update, in this case: Expiration date to 12/25/2017
		SecretAttributes secretAttr = new SecretAttributes();
		secretAttr.withExpires(new DateTime(2017, 12, 25, 0, 0));

		//Create UpdateSecretRequest for the updateSecret method, and call updateSecret
		UpdateSecretRequest usr = new UpdateSecretRequest.Builder(vaultURL, secretName).withAttributes(secretAttr).build();
		vc.updateSecret(usr);

    }

	/**
	* Retrieves a key's KeyBundle item
	*
	* @param vaultURL URL of the vault in Azure that the key exists in
	* @param keyName Name of the key that is to be retrieved
	*/

	public void showKey(String vaultURL, String keyName) throws IOException {
		KeyBundle keyBundle = vc.getKey(vaultURL, keyName);
	}

	/**
	* Retrieves a secret's SecretBundle item
	*
	* @param vaultURL URL of the vault in Azure that the secret exists in
	* @param keyName Name of the secret that is to be retrieved
	*/

	public void showSecret(String vaultURL, String secretName) throws IOException {
		SecretBundle secretBundle = vc.getSecret(vaultURL, secretName);
	}

	/**
	* Retrieves all keys from a vault
	*
	* @param vaultURL URL of the vault in Azure that the keys belong to
	*/

    public void listKeys(String vaultURL) throws IOException {
		PagedList<KeyItem> keyList = vc.listKeys(vaultURL);
    }

	/**
	* Retrieves all secrets from a vault
	*
	* @param vaultURL URL of the vault in Azure that the secrets belong to
	*/

    public void listSecrets(String vaultURL) throws IOException {
		PagedList<SecretItem> secretList = vc.listSecrets(vaultURL);
    }

	/**
	* Encrypts text using the specified key
	*
	* @param vaultURL URL of the vault in Azure that the key belong to
	* @param keyName Name of the key to use for the encryption
	* @param textToEncrypt String to encrypt, converted to UTF-16 byte array
	*/

	public KeyOperationResult encryptData(String vaultURL, String keyName, String textToEncrypt) throws IOException {
		String keyIdentifier = vaultURL + "/keys/" + keyName;
		byte[] byteText = textToEncrypt.getBytes("UTF-16");

		KeyOperationResult result = vc.encrypt(keyIdentifier, JsonWebKeyEncryptionAlgorithm.RSA_OAEP, byteText);

		return result;
	}

	/**
	* Decrypts data using the specified key
	*
	* @param vaultURL URL of the vault in Azure that the key belong to
	* @param keyName Name of the key to use for the encryption
	* @param dataToDecrypt byte[] of data to decrypt
	*/

	public void decryptData(String vaultURL, String keyName, byte[] dataToDecrypt) throws IOException {
		String keyIdentifier = vaultURL + "/keys/" + keyName;

		KeyOperationResult newResult = vc.decrypt(keyIdentifier, JsonWebKeyEncryptionAlgorithm.RSA_OAEP, dataToDecrypt);
		String decryptedResult = new String(newResult.result(), "UTF-16");

	}

	/**
	* Deletes a key
	*
	* @param vaultURL URL of the vault in Azure that the key exists in
	* @param keyName Name of the key that is to be deleted
	*/

    public void deleteKey(String vaultURL, String keyName) throws IOException {
		vc.deleteKey(vaultURL, keyName);

    }

	/**
	* Deletes a secret
	*
	* @param vaultURL URL of the vault in Azure that the secret exists in
	* @param secretName Name of the secret that is to be deleted
	*/

    public void deleteSecret(String vaultURL, String secretName) throws IOException {
        vc.deleteSecret(vaultURL, secretName);
    }
}

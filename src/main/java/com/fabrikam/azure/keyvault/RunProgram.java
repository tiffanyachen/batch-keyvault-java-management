package com.fabrikam.azure.keyvault;

import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.management.Azure;
import java.io.File;
import com.microsoft.azure.management.keyvault.Vault;
import okhttp3.logging.HttpLoggingInterceptor;

public class RunProgram 
{


    public static void main(String[] args)
    { 
        String PATH_TO_PROPS = "azureauth.properties";
        String vaultName = "manageKeyVault";
        String resourceGroupName = "resourceGroup";
        String vaultURL = "";
        String keyName = "keyOperationsKey";
        String secretName = "secretOperationsSecret";
        JsonWebKeyType jsonWebKeyType = JsonWebKeyType.RSA;
        String secretValue = "secretValue";
        String textToEncrypt = "Encrypt Text";
        String APP_ID = "";
        String SECRET_KEY = "";

        try {
        //Auth and get Azure Subscription for management plane
        Azure azure = Azure.configure().withLogLevel(HttpLoggingInterceptor.Level.NONE).authenticate(new File(PATH_TO_PROPS)).withDefaultSubscription();
        System.out.println(azure.subscriptionId());

        //Auth and get KeyVaultClient for operational plane
		KeyVaultCredentials kvCred = new ClientSecretKeyVaultCredential(APP_ID, SECRET_KEY);
		KeyVaultClient vc = new KeyVaultClient(kvCred);

        KeyVaultHandler kvh = new KeyVaultHandler(vc);
        KeyVaultManager kvm = new KeyVaultManager(azure);

            //Management Plane Operations
            Vault vault = kvm.createKeyVault(vaultName, resourceGroupName);
            vault = kvm.authorizeApp(vault, PATH_TO_PROPS);
            vault = kvm.updatePermissions(vault);
            kvm.listVaults(resourceGroupName);
            kvm.deleteVault(vault);

            //Key Operations
            kvh.createKey(vaultURL, keyName, jsonWebKeyType);
            kvh.updateKey(vaultURL, keyName);
            kvh.showKey(vaultURL, keyName);
            kvh.listKeys(vaultURL);
            KeyOperationResult kor = kvh.encryptData(vaultURL, keyName, textToEncrypt);
            kvh.decryptData(vaultURL, keyName, kor.result());
            kvh.deleteKey(vaultURL, keyName);
        
            //Secret Operations
            kvh.createSecret(vaultURL, secretName, secretValue);
            kvh.updateSecret(vaultURL, secretName);
            kvh.showSecret(vaultURL, secretName);
            kvh.listSecrets(vaultURL);
            kvh.deleteSecret(vaultURL, secretName);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
        
        System.out.println("DONE");
        System.exit(0);


    }
}

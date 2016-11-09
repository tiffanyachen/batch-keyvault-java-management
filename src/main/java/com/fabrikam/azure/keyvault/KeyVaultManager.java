package com.fabrikam.azure.keyvault;

/**
 * Hello world!
 *
 */

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.keyvault.KeyPermissions;
import com.microsoft.azure.management.keyvault.SecretPermissions;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceNamer;
import okhttp3.logging.HttpLoggingInterceptor;
import java.io.File;
import java.util.*;

import com.microsoft.azure.management.keyvault.AccessPolicy;
import com.google.common.base.Joiner;

public class KeyVaultManager 
{

    public static ArrayList<KeyPermissions> keyPermissions;
    public static ArrayList<SecretPermissions> secretPermissions;
    public static Azure azure;
    public static Vault vault;

    public KeyVaultManager(Azure azure) {
        this.azure = azure;
    }

    /**
	* Creates a Key Vault with the specified name in the specified resource group
	*
	* @param vaultName Name to be given to the Key Vault
	* @param resourceGroupName Name of the resource group that the Key Vault will belong to
	* 
    * @return Vault item containing created Key Vault
	*/

    public Vault createKeyVault(String vaultName, String resourceGroupName) throws Exception{
        vault = azure.vaults()
                    .define(vaultName)
                    .withRegion(Region.US_WEST)
                    .withNewResourceGroup(resourceGroupName)
                    .withEmptyAccessPolicy()
                    .create();

        return vault;
    }

    /**
	* Uses application token credentials from a properties file to authorize the application with the Key Vault
	*
	* @param vaultName Name of the Key Vault to give authorization
	* @param propsFilePath String containing path to file that contains properties for creating application token
	* 
    * @return Vault item containing created Key Vault that is granting permissions
	*/
            
    public Vault authorizeApp(Vault vault, String propsFilePath) throws Exception {
        vault = vault.update()
                    .defineAccessPolicy()
                    .forServicePrincipal(ApplicationTokenCredentials.fromFile(new File(propsFilePath)).getClientId())
                    .allowKeyAllPermissions()
                    .allowSecretPermissions(SecretPermissions.GET)
                    .allowSecretPermissions(SecretPermissions.LIST)
                    .attach()
                    .apply();
        return vault;
    }

    /**
	* Updates permissions for Key Vault
	*
	* @param vaultName Name of the key vault to update permissions to
	* 
    * @return Vault item with updated permissions
	*/

    public Vault updatePermissions(Vault vault) throws Exception {
        vault = vault.update()
                    .withDeploymentEnabled()
                    .withTemplateDeploymentEnabled()
                    .updateAccessPolicy(vault.accessPolicies().get(0).objectId())
                        .allowSecretAllPermissions()
                        .parent()
                    .apply();
        return vault;
    }

    /**
	* Lists all vaults in a resource group
	*
	* @param resourceGroupName String containing name of the resource group to lists all vaults from
	*/

    public void listVaults(String resourceGroupName) throws Exception {
        //List vaults
        System.out.println("listing key vaults....");
        for (Vault vault : azure.vaults().listByGroup(resourceGroupName)) {
            print(vault);
        }
    }

    /**
	* Deletes a Key Vault
	*
	* @param vaultName Name of the key vault to be deleted
	*/

    public void deleteVault(Vault vault) throws Exception {
        azure.vaults().delete(vault.id());
    }



    /**
     * Print a key vault.
     * @param vault the key vault resource
     */
    public static void print(Vault vault) {
        StringBuilder info = new StringBuilder().append("Key Vault: ").append(vault.id())
                .append("Name: ").append(vault.name())
                .append("\n\tResource group: ").append(vault.resourceGroupName())
                .append("\n\tRegion: ").append(vault.region())
                .append("\n\tSku: ").append(vault.sku().name()).append(" - ").append(vault.sku().family())
                .append("\n\tVault URI: ").append(vault.vaultUri())
                .append("\n\tAccess policies: ");
        for (AccessPolicy accessPolicy: vault.accessPolicies()) {
            info.append("\n\t\tIdentity:").append(accessPolicy.objectId())
                    .append("\n\t\tKey permissions: ").append(Joiner.on(", ").join(accessPolicy.permissions().keys()))
                    .append("\n\t\tSecret permissions: ").append(Joiner.on(", ").join(accessPolicy.permissions().secrets()));
        }
        System.out.println(info.toString());
    }
}

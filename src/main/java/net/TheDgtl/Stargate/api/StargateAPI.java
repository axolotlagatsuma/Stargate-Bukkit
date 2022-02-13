package net.TheDgtl.Stargate.api;

import net.TheDgtl.Stargate.Stargate;
import net.TheDgtl.Stargate.database.StorageAPI;
import net.TheDgtl.Stargate.network.StargateRegistryAPI;

/**
 * An API to facilitate addons and integrations
 *
 * <p>API specs currently being discussed on issue #77</p>
 *
 * @author Thorin
 */
public class StargateAPI {

    
    public static StargateRegistryAPI getRegistry() {
        return Stargate.getRegistry();
    }

    public static StorageAPI getStorageAPI() {
        return Stargate.getStorageAPI();
    }
    

}

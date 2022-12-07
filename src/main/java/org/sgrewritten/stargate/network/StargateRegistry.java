package org.sgrewritten.stargate.network;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.sgrewritten.stargate.Stargate;
import org.sgrewritten.stargate.action.SupplierAction;
import org.sgrewritten.stargate.config.ConfigurationHelper;
import org.sgrewritten.stargate.config.ConfigurationOption;
import org.sgrewritten.stargate.database.StorageAPI;
import org.sgrewritten.stargate.exception.NameErrorException;
import org.sgrewritten.stargate.exception.database.StorageReadException;
import org.sgrewritten.stargate.exception.database.StorageWriteException;
import org.sgrewritten.stargate.gate.structure.GateStructureType;
import org.sgrewritten.stargate.network.portal.BlockLocation;
import org.sgrewritten.stargate.network.portal.Portal;
import org.sgrewritten.stargate.network.portal.PortalFlag;
import org.sgrewritten.stargate.network.portal.RealPortal;
import org.sgrewritten.stargate.util.NameHelper;
import org.sgrewritten.stargate.vectorlogic.VectorUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Register of all portals and networks
 *
 * @author Thorin (idea from EpicKnarvik)
 */
public class StargateRegistry implements RegistryAPI {

    private final StorageAPI storageAPI;
    private final HashMap<String, Network> networkMap = new HashMap<>();
    private final HashMap<String, Network> bungeeNetworkMap = new HashMap<>();
    private final Map<GateStructureType, Map<BlockLocation, RealPortal>> portalFromStructureTypeMap = new EnumMap<>(GateStructureType.class);

    /**
     * Instantiates a new Stargate registry
     *
     * @param storageAPI <p>The database API to use for interfacing with the database</p>
     */
    public StargateRegistry(StorageAPI storageAPI) {
        this.storageAPI = storageAPI;
    }

    @Override
    public void loadPortals() {
        try {
            storageAPI.loadFromStorage(this);
        } catch (StorageReadException e) {
            e.printStackTrace();
            return;
        }
        Stargate.addSynchronousTickAction(new SupplierAction(() -> {
            updateAllPortals();
            return true;
        }));
    }

    @Override
    public void removePortal(Portal portal, PortalType portalType) {
        try {
            storageAPI.removePortalFromStorage(portal, portalType);
        } catch (StorageWriteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void savePortal(RealPortal portal, PortalType portalType) {
        try {
            storageAPI.savePortalToStorage(portal, portalType);
        } catch (StorageWriteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Network createNetwork(String networkName, NetworkType type, boolean isInterserver) throws NameErrorException {
        networkName = NameHelper.getTrimmedName(networkName);
        if (this.networkExists(networkName, isInterserver)) {
            throw new NameErrorException(null);
        }
        Network network = storageAPI.createNetwork(networkName, type, isInterserver);
        network.assignToRegistry(this);
        getNetworkMap(isInterserver).put(network.getId(), network);
        Stargate.log(Level.FINEST, String.format("Adding networkid %s to interServer = %b", network.getId(), isInterserver));
        return network;
    }
    
    @Override
    public Network createNetwork(String targetNetwork, Set<PortalFlag> flags) throws NameErrorException {
        return this.createNetwork(targetNetwork, NetworkType.getNetworkTypeFromFlags(flags),flags.contains(PortalFlag.FANCY_INTER_SERVER));
    }

    @Override
    public void updateAllPortals() {
        updatePortals(getNetworkMap());
        updatePortals(getBungeeNetworkMap());
    }

    @Override
    public void updatePortals(Map<String, ? extends Network> networkMap) {
        for (Network network : networkMap.values()) {
            network.updatePortals();
        }
    }

    @Override
    public boolean networkExists(String networkName, boolean isBungee) {
        return getNetwork(networkName, isBungee) != null;
    }

    @Override
    public Network getNetwork(String name, boolean isBungee) {
        String cleanName = name.trim().toLowerCase();
        if (ConfigurationHelper.getBoolean(ConfigurationOption.DISABLE_CUSTOM_COLORED_NAMES)) {
            cleanName = ChatColor.stripColor(cleanName);
        }
        return getNetworkMap(isBungee).get(cleanName);
    }

    @Override
    public RealPortal getPortal(BlockLocation blockLocation, GateStructureType structureType) {
        if (!(portalFromStructureTypeMap.containsKey(structureType))) {
            return null;
        }
        return portalFromStructureTypeMap.get(structureType).get(blockLocation);
    }

    @Override
    public RealPortal getPortal(BlockLocation blockLocation, GateStructureType[] structureTypes) {
        for (GateStructureType key : structureTypes) {
            RealPortal portal = getPortal(blockLocation, key);
            if (portal != null) {
                return portal;
            }
        }
        return null;
    }

    @Override
    public RealPortal getPortal(Location location, GateStructureType structureType) {
        return getPortal(new BlockLocation(location), structureType);
    }

    @Override
    public RealPortal getPortal(Location location, GateStructureType[] structureTypes) {
        return getPortal(new BlockLocation(location), structureTypes);
    }

    @Override
    public RealPortal getPortal(Location location) {
        return getPortal(location, GateStructureType.values());
    }


    @Override
    public boolean isPartOfPortal(List<Block> blocks) {
        for (Block block : blocks) {
            if (getPortal(block.getLocation()) != null) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isNextToPortal(Location location, GateStructureType structureType) {
        for (BlockVector adjacentVector : VectorUtils.getAdjacentRelativePositions()) {
            Location adjacentLocation = location.clone().add(adjacentVector);
            if (getPortal(adjacentLocation, structureType) != null) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void registerLocations(GateStructureType structureType, Map<BlockLocation, RealPortal> locationsMap) {
        if (!portalFromStructureTypeMap.containsKey(structureType)) {
            portalFromStructureTypeMap.put(structureType, new HashMap<>());
        }
        portalFromStructureTypeMap.get(structureType).putAll(locationsMap);
    }

    @Override
    public void unRegisterLocation(GateStructureType structureType, BlockLocation blockLocation) {
        Map<BlockLocation, RealPortal> map = portalFromStructureTypeMap.get(structureType);
        if (map != null && map.get(blockLocation) != null) {
            Stargate.log(Level.FINER, "Unregistering portal " + map.get(blockLocation).getName() +
                    " with structType " + structureType + " at location " + blockLocation.toString());
            map.remove(blockLocation);
        }
    }

    /**
     * Gets the map storing all networks of the given type
     *
     * @param getBungee <p>Whether to get BungeeCord networks</p>
     * @return <p>A network name -> network map</p>
     */
    private Map<String, Network> getNetworkMap(boolean getBungee) {
        if (getBungee) {
            return getBungeeNetworkMap();
        } else {
            return getNetworkMap();
        }
    }

    @Override
    public HashMap<String, Network> getBungeeNetworkMap() {
        return bungeeNetworkMap;
    }

    @Override
    public HashMap<String, Network> getNetworkMap() {
        return networkMap;
    }

    public void load() {
        networkMap.clear();
        bungeeNetworkMap.clear();
        portalFromStructureTypeMap.clear();
        this.loadPortals();
    }

}

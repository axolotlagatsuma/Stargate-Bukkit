package org.sgrewritten.stargate.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.sgrewritten.stargate.Stargate;
import org.sgrewritten.stargate.api.database.StorageAPI;
import org.sgrewritten.stargate.api.network.RegistryAPI;
import org.sgrewritten.stargate.api.network.portal.*;
import org.sgrewritten.stargate.exception.database.StorageWriteException;

import javax.sound.sampled.Port;
import java.util.*;
import java.util.logging.Level;

public class BlockHandlerResolver {
    private final Map<Material,List<BlockHandlerInterface>> blockHandlerMap = new HashMap<>();
    private final Map<BlockLocation,BlockHandlerInterface> blockBlockHandlerMap = new HashMap<>();
    private final StorageAPI storageAPI;
    private final Set<Character> customFlags = new HashSet<>();

    public BlockHandlerResolver(@NotNull StorageAPI storageAPI){
        this.storageAPI = Objects.requireNonNull(storageAPI);
    }

    /**
     * Add a listener for block placement next by a portal
     *
     * @param blockHandlerInterface A listener for block placement next by a portal
     */
    public void addBlockHandlerInterface(BlockHandlerInterface blockHandlerInterface) {
        List<BlockHandlerInterface> blockHandlerInterfaceList = this.blockHandlerMap.computeIfAbsent(blockHandlerInterface.getHandledMaterial(), k -> new ArrayList<>());
        blockHandlerInterfaceList.add(blockHandlerInterface);
        blockHandlerInterfaceList.sort(Comparator.comparingInt((ablockHandlerInterface) -> -ablockHandlerInterface.getPriority().getPriorityValue()));
    }

    /**
     * Remove a listener for block placement next by a portal
     *
     * @param blockHandlerInterface listener for block placement next by a portal
     */
    public void removeBlockHandlerInterface(BlockHandlerInterface blockHandlerInterface) {
        for(Material key : this.blockHandlerMap.keySet()){
            List<BlockHandlerInterface> blockHandlerInterfaceList = this.blockHandlerMap.get(key);
            if(blockHandlerInterfaceList.remove(blockHandlerInterface)){
                return;
            }
        }
    }

    /**
     * Remove all listeners for block placement next by a portal
     * @param plugin The plugin to remove listeners from
     */
    public void removeBlockHandlerInterfaces(Plugin plugin) {
        for(Material key : this.blockHandlerMap.keySet()){
            List<BlockHandlerInterface> blockHandlerInterfaceList = this.blockHandlerMap.get(key);
            blockHandlerInterfaceList.removeIf(blockHandlerInterface -> blockHandlerInterface.getPlugin() == plugin);
        }
    }

    /**
     *
     * @param location The location of the block that is being placed
     * @param portals The portal to try registration on
     * @param material The material of the block
     * @param player The player that placed the block
     */
    public void registerPlacement(RegistryAPI registry, Location location, List<RealPortal> portals, Material material, Player player) {
        if(!blockHandlerMap.containsKey(material)){
            return;
        }
        for(RealPortal portal : portals) {
            for(BlockHandlerInterface blockHandlerInterface : blockHandlerMap.get(material)){
                MetaData metaData = new MetaData("");
                if(portal.hasFlag(blockHandlerInterface.getFlag()) && blockHandlerInterface.registerBlock(location,player,portal,metaData)){
                    PortalPosition portalPosition = registry.savePortalPosition(portal,location,blockHandlerInterface.getInterfaceType(),blockHandlerInterface.getPlugin());
                    registry.registerPortalPosition(portalPosition,location,portal);
                    blockBlockHandlerMap.put(new BlockLocation(location),blockHandlerInterface);
                    if(!metaData.getMetaDataString().isEmpty()) {
                        portalPosition.setMetaData(portal, metaData.getMetaDataString());
                    }
                    return;
                }
            }
        }

    }

    /**
     *
     * @param location The location of the block that is being removed
     * @param portal The portal to try removal on
     */
    public void registerRemoval(RegistryAPI registry, Location location, RealPortal portal) {
        BlockHandlerInterface blockHandlerInterface = this.blockBlockHandlerMap.get(new BlockLocation(location));
        if(blockHandlerInterface == null){
            return;
        }
        blockHandlerInterface.unRegisterBlock(location,portal);
        registry.removePortalPosition(location);
    }

    /**
     * Method used for performance
     * @param material The material
     * @return Whether there exists a BlockHandlerInterface that
     * has registered for the material
     */
    public boolean hasRegisteredBlockHandler(Material material) {
        return blockHandlerMap.containsKey(material);
    }

    /**
     * Register a custom flag to stargate.
     *
     * @param flagCharacter <p> The character of the custom flag</p>
     */
    public void registerCustomFlag(char flagCharacter) throws IllegalArgumentException{
        if(Character.isLowerCase(flagCharacter)){
            throw new IllegalArgumentException("Character can't be lowercase");
        }

        for(PortalFlag flag : PortalFlag.values()){
            if(flagCharacter == flag.getCharacterRepresentation()){
                throw new IllegalArgumentException("Flag conflict with core flags");
            }
        }

        for(Character character : customFlags){
            if(flagCharacter == character){
                throw new IllegalArgumentException("Custom flag conflicts with another custom flag");
            }
        }

        try {
            storageAPI.addFlagType(flagCharacter);
            customFlags.add(flagCharacter);
        } catch (StorageWriteException ignored) {}
    }
}

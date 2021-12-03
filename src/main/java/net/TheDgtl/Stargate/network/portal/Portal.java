package net.TheDgtl.Stargate.network.portal;

import net.TheDgtl.Stargate.Bypass;
import net.TheDgtl.Stargate.PermissionManager;
import net.TheDgtl.Stargate.Setting;
import net.TheDgtl.Stargate.Settings;
import net.TheDgtl.Stargate.Stargate;
import net.TheDgtl.Stargate.TranslatableMessage;
import net.TheDgtl.Stargate.actions.DelayedAction;
import net.TheDgtl.Stargate.actions.SupplierAction;
import net.TheDgtl.Stargate.event.StargateCreateEvent;
import net.TheDgtl.Stargate.event.StargateOpenEvent;
import net.TheDgtl.Stargate.exception.GateConflict;
import net.TheDgtl.Stargate.exception.InvalidStructure;
import net.TheDgtl.Stargate.exception.NameError;
import net.TheDgtl.Stargate.exception.NoFormatFound;
import net.TheDgtl.Stargate.gate.Gate;
import net.TheDgtl.Stargate.gate.GateFormat;
import net.TheDgtl.Stargate.gate.structure.GateStructureType;
import net.TheDgtl.Stargate.network.Network;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * The parent class for every portal that interacts with server worlds
 *
 * @author Thorin
 */
public abstract class Portal implements IPortal {
    /**
     *
     */
    protected Network network;
    /**
     * Behaviours: - Cycle through PortalStates, make current state listener for
     * movements - (Constructor) Check validity, write sign, add self to a list in
     * the network
     * <p>
     * Added behaviours - (Listener) Listen for stargate clock (maybe 1 tick per
     * minute or something) maybe follow an external script that gives when the
     * states should change
     */
    final int openDelay = 20; // seconds
    private Gate gate;
    private final EnumSet<PortalFlag> flags;
    final String name;
    UUID openFor;
    IPortal destination = null;
    private long openTime = -1;
    private final UUID ownerUUID;
    protected PortalColorParser colorDrawer;


    Portal(Network network, String name, Block sign, EnumSet<PortalFlag> flags, UUID ownerUUID)
            throws NameError, NoFormatFound, GateConflict {
        this.ownerUUID = ownerUUID;
        this.network = network;
        this.name = name;
        this.flags = flags;

        if (!(Tag.WALL_SIGNS.isTagged(sign.getType()))) {
            throw new NoFormatFound();
        }
        /*
         * Get the block behind the sign; the material of that block is stored in a
         * register with available gateFormats
         */
        Directional signDirection = (Directional) sign.getBlockData();
        Block behind = sign.getRelative(signDirection.getFacing().getOppositeFace());
        List<GateFormat> gateFormats = GateFormat.getPossibleGatesFromControlBlockMaterial(behind.getType());
        setGate(FindMatchingGate(gateFormats, sign.getLocation(), signDirection.getFacing()));

        if (name.trim().isEmpty() || (name.length() == Stargate.MAX_TEXT_LENGTH))
            throw new NameError(TranslatableMessage.INVALID_NAME);
        if (this.network.isPortalNameTaken(name)) {
            throw new NameError(TranslatableMessage.ALREADY_EXIST);
        }

        this.colorDrawer = new PortalColorParser((Sign) getSignPos().getBlock().getState());

        if (gate.getFormat().isIronDoorBlockable) {
            flags.add(PortalFlag.IRON_DOOR);
        }

        StringBuilder msg = new StringBuilder("Selected with flags ");
        for (PortalFlag flag : flags) {
            msg.append(flag.label);
        }
        Stargate.log(Level.FINE, msg.toString());

        if (hasFlag(PortalFlag.ALWAYS_ON))
            this.open(null);
    }

    /**
     * Look through every stored gateFormat, checks every possible rotation / flip
     *
     * @param gateFormats
     * @param signLocation
     * @param signFacing
     * @return A gate with matching format
     * @throws NoFormatFound
     * @throws GateConflict
     */
    private Gate FindMatchingGate(List<GateFormat> gateFormats, Location signLocation, BlockFace signFacing)
            throws NoFormatFound, GateConflict {
        Stargate.log(Level.FINE, "Amount of GateFormats: " + gateFormats.size());
        for (GateFormat gateFormat : gateFormats) {
            Stargate.log(Level.FINE, "--------- " + gateFormat.name + " ---------");
            try {
                return new Gate(gateFormat, signLocation, signFacing, this);
            } catch (InvalidStructure ignored) {
            }
        }
        throw new NoFormatFound();
    }

    public HashMap<SGLocation, Portal> generateLocationHashMap(List<SGLocation> locations) {
        HashMap<SGLocation, Portal> output = new HashMap<>();
        for (SGLocation loc : locations) {
            output.put(loc, this);
        }
        return output;
    }

    @Override
    public Location getSignPos() {
        return gate.getSignLoc();
    }


    @Override
    public void update() {
        if (isOpen() && loadDestination() == null) {
            close(false);
        }
        drawControlMechanism();
    }

    /**
     * 
     * @param action
     * @param actor
     * @return <p> What the result of the event</p>
     */
    public Event.Result onSignClick(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        PermissionManager permissionManager = new PermissionManager(event.getPlayer());
        StargateCreateEvent colorSignPermission = new StargateCreateEvent(event.getPlayer(),this,new String[]{""},0);
        if (!itemIsColor(item) || !permissionManager.hasPerm(colorSignPermission)) {
            return Event.Result.DENY;
        }
        this.colorDrawer = new PortalColorParser((Sign) getSignPos().getBlock().getState());
        this.drawControlMechanism();
        return Event.Result.DEFAULT;
    }

    private boolean itemIsColor(ItemStack item) {
        if(item == null)
            return false;
        
        String itemName = item.getType().toString();
        return (itemName.contains("DYE") || itemName.contains("GLOW_INK_SAC"));
    }

    public abstract void drawControlMechanism();

    public abstract IPortal loadDestination();

    public boolean isOpen() {
        return getGate().isOpen();
    }

    public EnumSet<PortalFlag> getFlags() {
        return flags;
    }

    /**
     * Remove all information stored on this gate
     */
    public void destroy() {
        close(true);
        this.network.removePortal(this, true);
        String[] lines = new String[]{name, "", "", ""};
        getGate().drawControlMechanism(lines, false);

        for (GateStructureType formatType : GateStructureType.values()) {
            for (SGLocation loc : this.getGate().getLocations(formatType)) {
                Stargate.log(Level.FINEST, "Unregistering type: " + formatType + " location, at: " + loc);
                network.unRegisterLocation(formatType, loc);
            }
        }
        network.updatePortals();
    }

    public void open(Player actor) {
        getGate().open();
        if (actor != null)
            this.openFor = actor.getUniqueId();
        if (hasFlag(PortalFlag.ALWAYS_ON)) {
            return;
        }
        long openTime = System.currentTimeMillis();
        this.openTime = openTime;


        // Create action which will close this portal
        Supplier<Boolean> action = () -> {
            close(openTime);
            return true;
        };

        // Make the action on a delay
        Stargate.syncSecPopulator.addAction(new DelayedAction(openDelay, action));
    }

    /**
     * Everytime most of the portals opens, there is going to be a scheduled event
     * to close it after a specific time. If a player enters the portal before this,
     * then it is going to close, but the scheduled close event is still going to be
     * there. And if the portal gets activated again, it is going to close
     * prematurely, because of this already scheduled event. Solution to avoid this
     * is to assign an open-time for each scheduled close event and only close if the
     * related open time matches with the most recent time the portal was opened.
     *
     * @param relatedOpenTime
     */
    public void close(long relatedOpenTime) {
        if (relatedOpenTime == openTime)
            close(false);
    }

    @Override
    public void close(boolean forceClose) {
        if (hasFlag(PortalFlag.ALWAYS_ON) && !forceClose)
            return;
        getGate().close();
        drawControlMechanism();
        openFor = null;
    }

    @Override
    public boolean isOpenFor(Entity target) {
        return ((openFor == null) || (target.getUniqueId() == openFor));
    }

    public Location getExit() {
        return gate.getExit();
    }

    public void setOverrideDestination(IPortal destination) {
        this.destination = destination;
    }

    public Network getNetwork() {
        return this.network;
    }

    public void setNetwork(Network net) {
        this.network = net;
        this.drawControlMechanism();
    }

    protected IPortal getFinalDestination() {
        if (destination == null)
            destination = loadDestination();
        return destination;
    }

    public void onButtonClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (this.hasFlag(PortalFlag.IRON_DOOR) && event.useInteractedBlock() == Result.DENY) {
            Block exitBlock = gate.getExit().add(gate.getFacing().getDirection()).getBlock();
            if (exitBlock.getType() == Material.IRON_DOOR) {
                Directional signDirection = (Directional) gate.getSignLoc().getBlock().getBlockData();
                Directional doorDirection = (Directional) exitBlock.getBlockData();
                if (signDirection.getFacing() == doorDirection.getFacing()) {
                    return;
                }
            }
        }

        IPortal destination = loadDestination();
        if (destination == null) {
            player.sendMessage(Stargate.languageManager.getMessage(TranslatableMessage.INVALID, true));
            return;
        }
        PermissionManager permissionManager = new PermissionManager(player);
        StargateOpenEvent oEvent = new StargateOpenEvent(player, this, false);
        if (!permissionManager.hasPerm(oEvent) || oEvent.isCancelled())
            return;

        this.destination = destination;
        open(player);
        destination.open(player);
    }

    public void onIrisEntrance(Entity target) {
        Stargate.log(Level.FINEST, "Trying to teleport entity, initial velocity: " + target.getVelocity());
        doTeleport(target);
    }

    public Gate getGate() {
        return gate;
    }

    public void setGate(Gate gate) {
        this.gate = gate;
    }

    @Override
    public void teleportHere(Entity target, Portal origin) {

        BlockFace portalFacing = gate.facing.getOppositeFace();
        if (flags.contains(PortalFlag.BACKWARDS))
            portalFacing = portalFacing.getOppositeFace();


        BlockFace enterFacing = null;
        int useCost = 0;
        if (origin != null) {
            /*
             * If player enters from back, then take that into consideration
             */
            enterFacing = origin.getGate().getFacing();
            Vector vec = origin.getGate().getRelativeVector(target.getLocation().add(new Vector(-0.5, 0, -0.5)));
            if (vec.getX() > 0) {
                enterFacing = enterFacing.getOppositeFace();
            }

            boolean shouldCharge = !(this.hasFlag(PortalFlag.FREE) || origin.hasFlag(PortalFlag.FREE))
                    && target instanceof Player && !target.hasPermission(Bypass.COST_USE.getPermissionString());
            useCost = shouldCharge ? Settings.getInteger(Setting.USE_COST) : 0;
        }

        Teleporter teleporter = new Teleporter(getExit(), origin, portalFacing, enterFacing, useCost);

        Supplier<Boolean> action = () -> {
            teleporter.teleport(target);
            return true;
        };
        Stargate.syncTickPopulator.addAction(new SupplierAction(action));

    }

    @Override
    public void doTeleport(Entity target) {
        IPortal destination = getFinalDestination();
        if (destination == null) {
            target.sendMessage(Stargate.languageManager.getMessage(TranslatableMessage.INVALID, true));
            teleportHere(target, this);
            return;
        }

        destination.teleportHere(target, this);
        destination.close(false);
        close(false);
    }

    public static IPortal createPortalFromSign(Network net, String[] lines, Block block, EnumSet<PortalFlag> flags, UUID ownerUUID)
            throws NameError, NoFormatFound, GateConflict {
        if (flags.contains(PortalFlag.BUNGEE))
            return new BungeePortal(net, lines[0], lines[1], lines[2], block, flags, ownerUUID);
        if (flags.contains(PortalFlag.RANDOM))
            return new RandomPortal(net, lines[0], block, flags, ownerUUID);
        if (flags.contains(PortalFlag.NETWORKED))
            return new NetworkedPortal(net, lines[0], block, flags, ownerUUID);
        return new FixedPortal(net, lines[0], lines[1], block, flags, ownerUUID);
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean hasFlag(PortalFlag flag) {
        return flags.contains(flag);
    }

    public String getAllFlagsString() {
        StringBuilder out = new StringBuilder();
        for (PortalFlag flag : flags) {
            out.append(flag.label);
        }
        return out.toString();
    }

    public String getDesignName() {
        return gate.getFormat().name;
    }

    @Override
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public boolean isOwner(Player player) {
        return ownerUUID.equals(player.getUniqueId());
    }
}
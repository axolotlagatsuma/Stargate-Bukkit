package net.TheDgtl.Stargate.listeners;

import java.util.EnumSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import net.TheDgtl.Stargate.LangMsg;
import net.TheDgtl.Stargate.PermissionManager;
import net.TheDgtl.Stargate.Setting;
import net.TheDgtl.Stargate.Stargate;
import net.TheDgtl.Stargate.actions.PopulatorAction;
import net.TheDgtl.Stargate.event.StargateDestroyEvent;
import net.TheDgtl.Stargate.exception.GateConflict;
import net.TheDgtl.Stargate.exception.NameError;
import net.TheDgtl.Stargate.exception.NoFormatFound;
import net.TheDgtl.Stargate.gate.GateStructureType;
import net.TheDgtl.Stargate.network.Network;
import net.TheDgtl.Stargate.network.portal.IPortal;
import net.TheDgtl.Stargate.network.portal.Portal;
import net.TheDgtl.Stargate.network.portal.PortalFlag;

public class BlockEventListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		
		// TODO Have a list of all possible portalMaterials and skip if not any of those
		Location loc = event.getBlock().getLocation();
		IPortal portal = Network.getPortal(loc, GateStructureType.FRAME);
		if (portal != null) {
			int cost = 0; // TODO economy manager
			StargateDestroyEvent dEvent = new StargateDestroyEvent(portal, event.getPlayer(), false, "", cost);
			PermissionManager permMngr = new PermissionManager(event.getPlayer());
			if (permMngr.hasPerm(dEvent)) {
				PopulatorAction action = new PopulatorAction() {

					@Override
					public void run(boolean forceEnd) {
						String msg = Stargate.langManager.getMessage(LangMsg.DESTROY, false);
						event.getPlayer().sendMessage(msg);
						
						portal.destroy();
						Stargate.log(Level.FINEST, "Broke the portal");
					}

					@Override
					public boolean isFinished() {
						return true;
					}
					
				};
				Stargate.syncTickPopulator.addAction(action);
				return;
			}

			String reason = Stargate.langManager.getMessage(permMngr.getDenyMsg(), true);
			event.getPlayer().sendMessage(reason);
			event.setCancelled(true);
			return;
		}
		if (Network.getPortal(loc, GateStructureType.CONTROLL) != null) {
			Stargate.log(Level.FINEST, " Canceled sign break event");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		// Check if a portal control block is selected
		// If so, cancel event if not shift clicking
		if(event.getPlayer().isSneaking())
			return;
		
		if(Network.getPortal(event.getBlockAgainst().getLocation(), GateStructureType.CONTROLL) == null)
			return;
		
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		Block block = event.getBlock();
		if (!(block.getBlockData() instanceof WallSign))
			return;

		String[] lines = event.getLines();
		String network = lines[2];
		Player player = event.getPlayer();
		EnumSet<PortalFlag> flags = PortalFlag.parseFlags(lines[3]);
		PermissionManager permMngr = new PermissionManager(player);

		if (network.isBlank())
			network = (String) Stargate.getSetting(Setting.DEFAULT_NET);
		boolean hasPerm = true;
		
		if(network.endsWith("]") && network.startsWith("[")) {
			network = network.substring(1, network.length()-1);
			flags.add(PortalFlag.FANCY_INTERSERVER);
		}
		
		if(flags.contains(PortalFlag.BUNGEE)) {
			network = "§§§§§§#BUNGEE#§§§§§§";
		}
		
		/*
		 * TODO: PERMISSION CHECK
		 */
		
		if(player.getName().equals(network))
			flags.add(PortalFlag.PERSONAL_NETWORK);
		
		
		Network selectedNet;
		try {
			selectedNet = selectNetwork(network,flags);
		} catch (NameError e1) {
			player.sendMessage(Stargate.langManager.getMessage(e1.getMsg(), true));
			return;
		}
		

		try {
			IPortal portal = Portal.createPortalFromSign(selectedNet, lines, block, flags);
			if (!hasPerm) {
				player.sendMessage(Stargate.langManager.getMessage(permMngr.getDenyMsg(), true));
				portal.destroy();
				return;
			}
			selectedNet.addPortal(portal,true);
			selectedNet.updatePortals();
			Stargate.log(Level.FINE, "A Gateformat matches");
			player.sendMessage(Stargate.langManager.getMessage(LangMsg.CREATE, false));
		} catch (NoFormatFound e) {
			Stargate.log(Level.FINE, "No Gateformat matches");
		} catch (GateConflict e) {
			player.sendMessage(Stargate.langManager.getMessage(LangMsg.GATE_CONFLICT, true));
		} catch (NameError e) {
			player.sendMessage(Stargate.langManager.getMessage(e.getMsg(), true));
		}
	}
	
	private Network selectNetwork(String name, EnumSet<PortalFlag> flags) throws NameError {
		try {
			if(flags.contains(PortalFlag.PERSONAL_NETWORK))
				name = Bukkit.getPlayer(name).getUniqueId().toString();
			Stargate.factory.createNetwork(name, flags);
		} catch (NameError e1) {
			LangMsg msg= e1.getMsg();
			if(msg != null) {
				throw e1;
			}
		}
		return Stargate.factory.getNetwork(name, flags.contains(PortalFlag.FANCY_INTERSERVER));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		// check if portal is affected, if so cancel
		if(Network.isInPortal(event.getBlocks(), GateStructureType.values()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		// check if portal is affected, if so cancel
		if(Network.isInPortal(event.getBlocks(), GateStructureType.values()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if(Network.isInPortal(event.blockList(), GateStructureType.values()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		// check if water or lava is flowing into a gate entrance?
		// if so, cancel
		Block to = event.getToBlock();
		Block from = event.getBlock();
		if ((Network.getPortal(to.getLocation(), GateStructureType.IRIS) != null)
				|| (Network.getPortal(from.getLocation(), GateStructureType.IRIS) != null))
			event.setCancelled(true);
	}
}

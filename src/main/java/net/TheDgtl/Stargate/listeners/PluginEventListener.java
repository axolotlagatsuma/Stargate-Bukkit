package net.TheDgtl.Stargate.listeners;

import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import net.TheDgtl.Stargate.Stargate;

public class PluginEventListener implements Listener{
	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginEnable(PluginEnableEvent event) {
		//check if vaults
		// TODO identify this behaviour
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
		//check if the plugin was the economyhandler and send a message
		/*
		 * if (event.getPlugin().equals(stargate.getEconomyHandler().getVault())) {
		 * Stargate.log("Vault plugin lost.", Level.INFO); }
		 */
	}
}

/*
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.TheDgtl.Stargate.event;

import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import net.TheDgtl.Stargate.portal.Portal;

public class StargateActivateEvent extends StargateEvent {
    private final Player player;
    private List<String> destinations;
    private String destination;

    private static final HandlerList handlers = new HandlerList();

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public StargateActivateEvent(@NotNull Portal portal, @NotNull Player player, @NotNull List<String> destinations, @NotNull String destination) {
        super(Objects.requireNonNull(portal));
        this.player = Objects.requireNonNull(player);
        this.destinations = Objects.requireNonNull(destinations);
        this.destination = Objects.requireNonNull(destination);
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(@NotNull List<String> destinations) {
        this.destinations = Objects.requireNonNull(destinations);
    }

    @NotNull
    public String getDestination() {
        return destination;
    }

    public void setDestination(@NotNull String destination) {
        this.destination = Objects.requireNonNull(destination);
    }
}
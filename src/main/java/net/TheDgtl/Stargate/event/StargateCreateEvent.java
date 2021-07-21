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

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import net.TheDgtl.Stargate.portal.Portal;

public class StargateCreateEvent extends StargateEvent {
    private final Player player;
    private boolean deny;
    private String denyReason;
    private final String[] lines;
    private int cost;

    private static final HandlerList handlers = new HandlerList();

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public StargateCreateEvent(@NotNull Player player, @NotNull Portal portal, @NotNull String[] lines, boolean deny, @NotNull String denyReason, int cost) {
        super(Objects.requireNonNull(portal));
        this.player = Objects.requireNonNull(player);
        this.lines = Objects.requireNonNull(lines);
        this.deny = deny;
        this.denyReason = Objects.requireNonNull(denyReason);
        this.cost = cost;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getLine(int index) throws IndexOutOfBoundsException {
        return lines[index];
    }

    public boolean getDeny() {
        return deny;
    }

    public void setDeny(boolean deny) {
        this.deny = deny;
    }

    @NotNull
    public String getDenyReason() {
        return denyReason;
    }

    public void setDenyReason(@NotNull String denyReason) {
        this.denyReason = Objects.requireNonNull(denyReason);
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

}

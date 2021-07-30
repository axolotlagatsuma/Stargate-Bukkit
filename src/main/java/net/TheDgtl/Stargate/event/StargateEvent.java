package net.TheDgtl.Stargate.event;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import net.TheDgtl.Stargate.portal.IPortal;

public abstract class StargateEvent extends Event implements Cancellable {
	// oldname = StargateEvent
    protected final IPortal portal;
    protected boolean cancelled;

    public StargateEvent(@NotNull IPortal portal) {
        this.portal = Objects.requireNonNull(portal);
        this.cancelled = false;
    }

    public IPortal getPortal() {
        return portal;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    //TODO temporary, this method should be abstract
	public List<String> getRelatedPerms(){return new ArrayList<>();}
}

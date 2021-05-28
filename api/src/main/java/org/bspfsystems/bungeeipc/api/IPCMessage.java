/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 *
 * Copyright (C) 2020  Matt Ciolkosz (https://github.com/mciolkosz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bspfsystems.bungeeipc.api;

import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public final class IPCMessage {
    
    public static final String BROADCAST_SERVER = "BROADCAST";
    
    private static final String SEPARATOR = "`|`";
    
    private final String server;
    private final String channel;
    private final ArrayList<String> data;
    
    private int lastRead;
    
    public IPCMessage(@NotNull final String server, @NotNull final String channel) {
        this(server, channel, new ArrayList<String>());
    }
    
    private IPCMessage(@NotNull final String server, @NotNull final String channel, @NotNull ArrayList<String> data) {
        
        validateNotBlank(server, "IPCMessage server cannot be blank.");
        validateNotBlank(channel, "IPCMessage channel cannot be blank.");
        
        this.server = server;
        this.channel = channel;
        this.data = data;
        
        this.lastRead = -1;
    }
    
    @NotNull
    public String getServer() {
        return this.server;
    }
    
    @NotNull
    public String getChannel() {
        return this.channel;
    }
    
    public boolean hasNext() {
        return this.lastRead < this.data.size() - 1;
    }
    
    @NotNull
    public String next() {
        this.lastRead++;
        return this.data.get(this.lastRead);
    }
    
    public boolean add(@NotNull final String message) {
        return this.data.add(message);
    }
    
    @NotNull
    public String write() {
        
        final StringBuilder builder = new StringBuilder();
        builder.append(this.server).append(SEPARATOR).append(this.channel);
    
        for (final String item : this.data) {
            builder.append(SEPARATOR).append(item);
        }
        
        return builder.toString();
    }
    
    @Override
    @NotNull
    public String toString() {
        return this.write();
    }
    
    @NotNull
    public static IPCMessage read(@NotNull String value) {
        
        validateNotBlank(value, "IPCMessage data cannot be blank, cannot recreate IPCMessage: " + value);
        final ArrayList<String> split = new ArrayList<String>();
        
        int index = value.indexOf(SEPARATOR);
        while (index != -1) {
            split.add(value.substring(0, index));
            value = value.substring(index + SEPARATOR.length());
            index = value.indexOf(SEPARATOR);
        }
        split.add(value);
        
        if (split.size() < 3) {
            throw new IllegalArgumentException("Cannot recreate IPCMessage, no server, channel, and data: " + value);
        }
        
        return new IPCMessage(split.remove(0), split.remove(0), split);
    }
    
    private static void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}

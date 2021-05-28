/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 * 
 * Copyright 2020-2021 BSPF Systems, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

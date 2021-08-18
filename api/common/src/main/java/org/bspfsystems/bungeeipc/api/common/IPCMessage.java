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

package org.bspfsystems.bungeeipc.api.common;

import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a message that is sent between {@link IPCSocket}s.
 * <p>
 * The {@link String} data stored in an {@link IPCMessage} has order maintained
 * via an {@link ArrayList}. The order that the data was added in will be the
 * order that the data can be read in. None of the data may be
 * <code>null</code>.
 */
public final class IPCMessage {
    
    public static final String BROADCAST_SERVER = "BROADCAST";
    
    private static final String SEPARATOR = "`|`";
    
    private final String server;
    private final String channel;
    private final ArrayList<String> data;
    
    private int lastRead;
    
    /**
     * Constructs a new {@link IPCMessage} with no data.
     * 
     * @param server The {@link IPCSocket} that the message is to be sent to
     *               ("proxy" if it is to go to the BungeeCord proxy
     *               {@link IPCPlugin}).
     * @param channel The channel that the message is to be read by.
     * @see IPCMessage#IPCMessage(String, String, ArrayList)
     */
    public IPCMessage(@NotNull final String server, @NotNull final String channel) {
        this(server, channel, new ArrayList<String>());
    }
    
    /**
     * Constructs a new {@link IPCMessage} with the given data (may be empty).
     *
     * @param server The {@link IPCSocket} that the message is to be sent to
     *               ("proxy" if it is to go to the BungeeCord proxy
     *               {@link IPCPlugin}).
     * @param channel The channel that the message is to be read by.
     * @param data The data to initialize the message with.
     */
    private IPCMessage(@NotNull final String server, @NotNull final String channel, @NotNull ArrayList<String> data) {
    
        IPCMessage.validateNotBlank(server, "IPCMessage IPC server cannot be blank.");
        IPCMessage.validateNotBlank(channel, "IPCMessage channel cannot be blank.");
        IPCMessage.validateNotNull(data, "IPCMessage data cannot have null entries: " + data.toString());
        
        this.server = server;
        this.channel = channel;
        this.data = data;
        
        this.lastRead = -1;
    }
    
    /**
     * Gets the name of the {@link IPCSocket} to send this {@link IPCMessage}
     * to.
     * 
     * @return The name of the {@link IPCSocket} to send this {@link IPCMessage}
     *         to.
     */
    @NotNull
    public String getServer() {
        return this.server;
    }
    
    /**
     * Gets the channel that this {@link IPCMessage} will be read by.
     * 
     * @return The channel that this {@link IPCMessage} will be read by.
     */
    @NotNull
    public String getChannel() {
        return this.channel;
    }
    
    /**
     * Checks to see if there is any remaining data to be read.
     * 
     * @return <code>true</code> if there is more data to be read,
     *         <code>false</code> otherwise.
     */
    public boolean hasNext() {
        return this.lastRead < this.data.size() - 1;
    }
    
    /**
     * Reads the next piece of data in this {@link IPCMessage}.
     * 
     * @return The next piece of data in this {@link IPCMessage}.
     * @throws IndexOutOfBoundsException If an attempt is made to read data
     *                                   after the end of the internal list
     *                                   has been reached.
     */
    @NotNull
    public String next() throws IndexOutOfBoundsException {
        this.lastRead++;
        return this.data.get(this.lastRead);
    }
    
    /**
     * Adds the next message to this {@link IPCMessage}.
     * 
     * @param message The next message to add to this {@link IPCMessage}.
     */
    public void add(@NotNull final String message) {
        this.data.add(message);
    }
    
    /**
     * Writes the data stored in the internal list out to a single
     * {@link String}.
     * <p>
     * This is usually used when sending an {@link IPCMessage} via an
     * {@link IPCSocket}.
     * 
     * @return The data in the internal list as a {@link String}.
     * @see IPCSocket#sendMessage(IPCMessage)
     */
    @NotNull
    public String write() {
        
        final StringBuilder builder = new StringBuilder();
        builder.append(this.server).append(SEPARATOR).append(this.channel);
    
        for (final String item : this.data) {
            builder.append(SEPARATOR).append(item);
        }
        
        return builder.toString();
    }
    
    /**
     * Writes the data stored in the internal list out to a single
     * {@link String}.
     * 
     * @return The data in the internal list as a {@link String}.
     * @see IPCMessage#write()
     */
    @Override
    @NotNull
    public String toString() {
        return this.write();
    }
    
    /**
     * Reads in a {@link String} that has been produced via the
     * {@link IPCMessage#write()} method, and re-creates a new
     * {@link IPCMessage} from it.
     * <p>
     * This is usually used after the data has been received by an
     * {@link IPCSocket}.
     * 
     * @param value The data to create an {@link IPCMessage} from.
     * @return The reconstructed {@link IPCMessage}.
     * @see IPCSocket#run()
     */
    @NotNull
    public static IPCMessage read(@NotNull String value) {
    
        IPCMessage.validateNotBlank(value, "IPCMessage data cannot be blank, cannot recreate IPCMessage: " + value);
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
    
    private static void validateNotNull(@NotNull final ArrayList<String> data, @NotNull final String message) {
        for (final String item : data) {
            if (item == null) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}

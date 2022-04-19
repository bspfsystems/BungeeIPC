/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 * 
 * Copyright 2020-2022 BSPF Systems, LLC
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

import java.util.NoSuchElementException;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a message that is sent between {@link IPCSocket IPCSockets}.
 * <p>
 * The {@link String} data stored in an {@link IPCMessage} has order maintained
 * via a {@link Queue}. The order that the data was added in will be the order
 * that the data can be read in. None of the data may be {@code null}.
 */
public interface IPCMessage {
    
    /**
     * Used to represent the BungeeCord proxy. This can be used as either an
     * origin or a destination.
     */
    String PROXY_SERVER = "PROXY";
    
    /**
     * Used to represent a message that should be broadcast to all Bukkit
     * servers. This can only be used as a destination.
     */
    String BROADCAST_SERVER = "BROADCAST";
    
    /**
     * Used as a placeholder when a message is sent by a Bukkit server, and has
     * not yet reached the BungeeCord proxy. This can only be used as an origin.
     * It will be automatically replaced when it is read in by the BungeeCord
     * proxy.
     */
    String PLACEHOLDER_SERVER = "%%SERVER%%";
    
    /**
     * Gets the origin {@link IPCSocket} of this {@link IPCMessage}.
     * <p>
     * This may be {@link IPCMessage#PLACEHOLDER_SERVER} if this
     * {@link IPCMessage} has been sent by a Bukkit server and has not yet
     * reached the BungeeCord proxy.
     * 
     * @return The origin {@link IPCSocket} of this {@link IPCMessage}.
     */
    @NotNull
    String getOrigin();
    
    /**
     * Gets the destination {@link IPCSocket} for this {@link IPCMessage}.
     * 
     * @return The destination {@link IPCSocket} for this {@link IPCMessage}.
     */
    @NotNull
    String getDestination();
    
    /**
     * Gets the channel that this {@link IPCMessage} will be read by.
     * 
     * @return The channel that this {@link IPCMessage} will be read by.
     */
    @NotNull
    String getChannel();
    
    /**
     * Adds the next message to this {@link IPCMessage}.
     *
     * @param message The next message to add to this {@link IPCMessage}.
     * @throws IllegalArgumentException If adding the message to this
     *                                  {@link IPCMessage} would make it too
     *                                  long to be sent.
     */
    void add(@NotNull final String message) throws IllegalArgumentException;
    
    /**
     * Checks to see if there is any remaining data to be read.
     * 
     * @return {@code true} if there is more data to be read, {@code false}
     *         otherwise.
     */
    boolean hasNext();
    
    /**
     * Reads the next piece of data in this {@link IPCMessage}.
     * 
     * @return The next piece of data in this {@link IPCMessage}.
     * @throws NoSuchElementException If an attempt is made to read data after
     *                                   the end of the internal {@link Queue}
     *                                   has been reached.
     */
    @NotNull
    String next();
    
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
    String write();
    
    /**
     * Writes the data stored in the internal list out to a single
     * {@link String}.
     * 
     * @return The data in the internal list as a {@link String}.
     * @see IPCMessage#write()
     */
    @Override
    @NotNull
    String toString();
}

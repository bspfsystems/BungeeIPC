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

package org.bspfsystems.bungeeipc.api.client;

import java.util.List;
import java.util.Queue;
import org.bspfsystems.bungeeipc.api.common.AbstractIPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the client-side extension of an {@link AbstractIPCMessage}.
 */
public final class ClientIPCMessage extends AbstractIPCMessage {
    
    /**
     * Constructs a new {@link IPCMessage} with no data that originates on a
     * Bukkit server.
     * 
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @throws IllegalArgumentException If {@code destination} and/or
     *                                  {@code channel} are blank.
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String)
     */
    public ClientIPCMessage(@NotNull final String destination, @NotNull final String channel) throws IllegalArgumentException, IllegalStateException {
        super(IPCMessage.PLACEHOLDER_SERVER, destination, channel);
    }
    
    /**
     * Constructs a new {@link IPCMessage} containing the initial data in the
     * given {@link List}. This message originates on a Bukkit server. Order of
     * the items in the {@link List} will be maintained.
     * 
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @param data The initial data as a {@link List}. Order will be maintained.
     * @throws IllegalArgumentException If {@code destination} and/or
     *                                  {@code channel} are blank, or if any
     *                                  element in {@code data} is {@code null}.
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, List)
     */
    public ClientIPCMessage(@NotNull final String destination, @NotNull final String channel, @NotNull final List<String> data) throws IllegalArgumentException, IllegalStateException {
        super(IPCMessage.PLACEHOLDER_SERVER, destination, channel, data);
    }
    
    /**
     * Constructs a new {@link IPCMessage} containing the initial data in the
     * given {@link Queue}. This message originates on a Bukkit server. Order of
     * the items in the {@link Queue} will be maintained.
     * 
     * @param destination The destination {@link IPCSocket}.
     * @param channel The channel the {@link IPCMessage} will be read by.
     * @param data The initial data as a {@link Queue}. Order will be
     *             maintained.
     * @throws IllegalArgumentException If {@code destination} and/or
     *                                  {@code channel} are blank, or if any
     *                                  element in {@code data} is {@code null}.
     * @throws IllegalStateException If the given parameters contain too much
     *                               data to send in a single
     *                               {@link IPCMessage}.
     * @see AbstractIPCMessage#AbstractIPCMessage(String, String, String, Queue)
     */
    public ClientIPCMessage(@NotNull final String destination, @NotNull final String channel, @NotNull final Queue<String> data) throws IllegalArgumentException, IllegalStateException {
        super(IPCMessage.PLACEHOLDER_SERVER, destination, channel, data);
    }
}

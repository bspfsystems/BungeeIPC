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

import org.jetbrains.annotations.NotNull;

/**
 * Represents the base IPC plugin for both Bukkit and Bungeecord.
 */
public interface IPCPlugin {
    
    /**
     * Attempts to subscribe an {@link IPCReader} to the specified channel,
     * returning the success.
     * 
     * @param channel The channel to subscribe to.
     * @param reader The {@link IPCReader} to subscribe.
     * @return {@code true} if the channel was subscribed to successfully,
     *         {@code false} if it was already subscribed to or overrode another
     *         subscription.
     */
    boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader);
    
    /**
     * Unsubscribes the specific channel, returning the success.
     * 
     * @param channel The channel to unsubscribe from.
     * @return {@code true} if the channel was unsubscribed from successfully,
     *         {@code false} otherwise.
     */
    boolean removeReader(@NotNull final String channel);
    
    /**
     * Sends the {@link IPCMessage} over the network connection to its
     * destination ({@link IPCMessage#getDestination()}).
     * 
     * @param message The {@link IPCMessage} to send.
     */
    void sendMessage(@NotNull final IPCMessage message);
    
    /**
     * Receives the {@link IPCMessage} and begins the processing of it.
     * 
     * @param message The {@link IPCMessage} to be processed.
     */
    void receiveMessage(@NotNull final IPCMessage message);
}

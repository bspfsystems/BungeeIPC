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

package org.bspfsystems.bungeeipc.api.server;

import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCPlugin;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the server-side version of the {@link IPCPlugin}.
 */
public interface ServerIPCPlugin extends IPCPlugin {
    
    /**
     * Checks whether the specified name is a registered
     * {@link ServerIPCSocket}.
     * <p>
     * This name may or may not align with the Minecraft server names that are
     * configured in the BungeeCord proxy, depending on the implementation.
     * 
     * @param name The name assigned to the {@link ServerIPCSocket}.
     * @return {@code true} if an {@link ServerIPCSocket} is registered with the
     *         name, {@code false} otherwise.
     */
    boolean isRegisteredServer(@NotNull final String name);
    
    /**
     * Checks to see if the specified {@link ServerIPCSocket} is running or
     * not.
     * 
     * @param name The name of the {@link ServerIPCSocket} to check.
     * @return {@code true} if the server is running, {@code false} if it is
     *         stopped.
     */
    boolean isServerRunning(@NotNull final String name);
    
    /**
     * Checks to see if the client side of the {@link ServerIPCSocket} has
     * connected or not.
     * <p>
     * This will always return {@code false} if
     * {@link ServerIPCPlugin#isServerRunning(String)} returns {@code false}.
     * 
     * @param name The name of the {@link ServerIPCSocket} to check.
     * @return {@code true} if the opposing {@link IPCSocket} has connected to
     *         the {@link ServerIPCSocket}, {@code false} if the connection has
     *         not been completed or the {@link ServerIPCSocket} is not running.
     */
    boolean isServerConnected(@NotNull final String name);
    
    /**
     * Restarts the specified {@link ServerIPCSocket}. This does not restart the
     * actual Minecraft server.
     * 
     * @param name The server to restart.
     */
    void restartServer(@NotNull final String name);
    
    /**
     * Broadcasts the given {@link IPCMessage} to all {@link ServerIPCSocket}s,
     * thus reaching all of the connected Minecraft servers.
     * 
     * @param message The {@link IPCMessage} to broadcast.
     */
    void broadcastMessage(@NotNull final IPCMessage message);
}

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

package org.bspfsystems.bungeeipc.api.plugin;

import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.socket.IPCServerSocket;
import org.bspfsystems.bungeeipc.api.socket.IPCSocket;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the server-side version of the {@link IPCPlugin}.
 */
public interface IPCServerPlugin extends IPCPlugin {
    
    /**
     * Checks whether the specified name is a registered
     * {@link IPCServerSocket}.
     * <p>
     * This name may or may not align with the Minecraft server names that are
     * configured in the BungeeCord proxy, depending on the implementation.
     * 
     * @param name The name assigned to the {@link IPCServerSocket} that is
     *             connected to the {@link IPCSocket} on the Minecraft server.
     * @return <code>true</code> if an {@link IPCServerSocket }is registered
     *         with the name, <code>false</code> otherwise.
     */
    boolean isRegisteredServer(@NotNull String name);
    
    /**
     * Checks to see if the specified {@link IPCServerSocket} is running or
     * not.
     * 
     * @param name The name of the {@link IPCServerSocket} to check.
     * @return <code>true</code> if the server is running,
     *         <code>false</code> if it is stopped.
     */
    boolean isServerRunning(@NotNull String name);
    
    /**
     * Checks to see if the client side of the {@link IPCServerSocket} has
     * connected or not.
     * <p>
     * This will always return <code>false</code> if
     * {@link IPCServerPlugin#isServerRunning(String)} returns
     * <code>false</code>.
     * 
     * @param name The name of the {@link IPCServerSocket} to check.
     * @return <code>true</code> if the {@link IPCSocket} has connected to the
     *         {@link IPCServerSocket}, <code>false</code> if the connection has
     *         not been completed or the {@link IPCServerSocket} is not running.
     */
    boolean isServerConnected(@NotNull String name);
    
    /**
     * Restarts the specified {@link IPCServerSocket}.This does not restart the
     * actual Minecraft server.
     * 
     * @param name The server to restart.
     */
    void restartServer(@NotNull String name);
    
    /**
     * Broadcasts the given {@link IPCMessage} to all {@link IPCServerSocket}s,
     * thus reaching all of the connected Minecraft servers.
     * 
     * @param message The {@link IPCMessage} to broadcast.
     */
    void broadcastMessage(@NotNull IPCMessage message);
}

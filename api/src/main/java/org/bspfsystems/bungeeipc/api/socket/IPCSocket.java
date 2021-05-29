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

package org.bspfsystems.bungeeipc.api.socket;

import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.plugin.IPCClientPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a connection for {@link IPCMessage}s. The {@link IPCSocket} by
 * itself is usually used on the {@link IPCClientPlugin} side of the connection
 * (the Minecraft server).
 */
public interface IPCSocket extends Runnable {
    
    /**
     * Gets whether this {@link IPCSocket} is running or not.
     * 
     * @return <code>true</code> if this {@link IPCSocket} is running,
     *         <code>false</code> if it is stopped.
     */
    boolean isRunning();
    
    /**
     * Gets whether this {@in IPCSocket} is connected to the
     * {@link IPCServerSocket} or not.
     * <p>
     * This will always return <code>false</code> if
     * {@link IPCSocket#isRunning()} returns <code>false</code>.
     * 
     * @return <code>true</code> if this {@link IPCSocket} is running and is
     * connected to the {@link IPCServerSocket}, <code>false</code> otherwise.
     */
    boolean isConnected();
    
    /**
     * Starts this {@link IPCSocket}, which will attempt to connect to the
     * {@link IPCServerSocket}.
     */
    void start();
    
    /**
     * Stops this {@link IPCSocket}, disconnecting it.
     */
    void stop();
    
    /**
     * Sends the specified {@link IPCMessage} to the connected
     * {@link IPCServerSocket}, as long as this is running and connected.
     * 
     * @param message The {@link IPCMessage} to send.
     */
    void sendMessage(@NotNull IPCMessage message);
}

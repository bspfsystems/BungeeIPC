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

import org.bspfsystems.bungeeipc.api.socket.IPCServerSocket;
import org.bspfsystems.bungeeipc.api.socket.IPCSocket;

/**
 * Represents the client-side version of the {@link IPCPlugin}.
 */
public interface IPCClientPlugin extends IPCPlugin {
    
    /**
     * Gets whether the client-side connection is running or not.
     * 
     * @return <code>true</code> if the client {@link IPCSocket} is running,
     *         <code>false</code> if it is stopped.
     */
    boolean isClientRunning();
    
    /**
     * Gets whether the client {@link IPCSocket} is connected to the
     * {@link IPCServerSocket} or not.
     * <p>
     * This will always return <code>false</code> if
     * {@link IPCClientPlugin#isClientRunning()} returns <code>false</code>.
     * 
     * @return <code>true</code> if the client {@link IPCSocket} is connected
     *         to the {@link IPCServerSocket}, <code>false</code> if the client
     *         is not connected or is not running.
     */
    boolean isClientConnected();
    
    /**
     * Restarts the client-side {@link IPCSocket}.
     */
    void restartClient();
}

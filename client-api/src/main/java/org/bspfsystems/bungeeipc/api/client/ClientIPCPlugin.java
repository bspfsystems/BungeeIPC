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

import org.bspfsystems.bungeeipc.api.common.IPCPlugin;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;

/**
 * Represents the client-side version of the {@link IPCPlugin}.
 */
public interface ClientIPCPlugin extends IPCPlugin {
    
    /**
     * Gets whether the client-side connection is running or not.
     * 
     * @return {@code true} if the client {@link IPCSocket} is running,
     *         {@code false} if it is stopped.
     */
    boolean isClientRunning();
    
    /**
     * Gets whether the {@link ClientIPCSocket} is connected or not.
     * <p>
     * This will always return {@code false} if
     * {@link ClientIPCPlugin#isClientRunning()} returns {@code false}.
     * 
     * @return {@code true} if the client {@link IPCSocket} is connected,
     *         {@code false} if the client is not connected or is not
     *         running.
     */
    boolean isClientConnected();
    
    /**
     * Restarts the client-side {@link IPCSocket}.
     */
    void restartClient();
}

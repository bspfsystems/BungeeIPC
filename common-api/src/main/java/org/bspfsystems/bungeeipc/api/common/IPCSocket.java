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
 * Represents a connection for {@link IPCMessage IPCMessages}.
 */
public interface IPCSocket extends Runnable {
    
    /**
     * Gets whether this {@link IPCSocket} is running or not.
     * 
     * @return {@code true} if this {@link IPCSocket} is running, {@code false}
     *         if it is stopped.
     */
    boolean isRunning();
    
    /**
     * Gets whether this {@link IPCSocket} is connected or not.
     * <p>
     * This will always return {@code false} if {@link IPCSocket#isRunning()}
     * returns {@code false}.
     * 
     * @return {@code true} if this {@link IPCSocket} is running and is
     *         connected, {@code false} otherwise.
     */
    boolean isConnected();
    
    /**
     * Starts this {@link IPCSocket}, which will attempt to connect.
     */
    void start();
    
    /**
     * Stops this {@link IPCSocket}, disconnecting it.
     */
    void stop();
    
    /**
     * Sends the specified {@link IPCMessage} to the connected
     * {@link IPCSocket}, as long as this is running and connected.
     * 
     * @param message The {@link IPCMessage} to send.
     */
    void sendMessage(@NotNull final IPCMessage message);
}

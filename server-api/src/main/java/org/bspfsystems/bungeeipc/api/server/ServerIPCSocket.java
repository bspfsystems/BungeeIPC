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
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a client connection for {@link IPCMessage}s. The
 * {@link ServerIPCSocket} is usually used on the {@link ServerIPCPlugin} side
 * of the connection (the BungeeCord proxy).
 */
public interface ServerIPCSocket extends IPCSocket {
    
    /**
     * Gets the name of this {@link ServerIPCSocket}.
     * <p>
     * This may or may not be the same name as the Minecraft server that is
     * configured in BungeeCord, depending on the implementation.
     * 
     * @return The name of this {@link ServerIPCSocket}.
     */
    @NotNull
    String getName();
}

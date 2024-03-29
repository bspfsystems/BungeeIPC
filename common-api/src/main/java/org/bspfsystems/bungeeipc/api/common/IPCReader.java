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
 * Represents an object that will read {@link IPCMessage}s after they are
 * received by {@link IPCSocket}s.
 */
public interface IPCReader {
    
    /**
     * Reads the specified {@link IPCMessage} and processes it.
     * 
     * @param message The {@link IPCMessage} to read and process.
     */
    void readMessage(@NotNull final IPCMessage message);
}

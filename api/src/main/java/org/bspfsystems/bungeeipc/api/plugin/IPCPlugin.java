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
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.jetbrains.annotations.NotNull;

public interface IPCPlugin {
    
    boolean addReader(@NotNull String channel, @NotNull IPCReader reader);
    boolean removeReader(@NotNull String channel);
    void sendMessage(@NotNull IPCMessage message);
    void receiveMessage(@NotNull IPCMessage message);
}

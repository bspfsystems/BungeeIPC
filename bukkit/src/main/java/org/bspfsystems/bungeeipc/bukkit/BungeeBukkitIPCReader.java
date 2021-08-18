/*
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
 *
 * Copyright (C) 2020-2021 BSPF Systems, LLC (https://bspfsystems.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bspfsystems.bungeeipc.bukkit;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCReader;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

final class BungeeBukkitIPCReader implements IPCReader {

    private final BukkitIPCPlugin ipcPlugin;
    private final Logger logger;
    
    BungeeBukkitIPCReader(@NotNull final BukkitIPCPlugin ipcPlugin) {
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
    }
    
    @Override
    public void readMessage(@NotNull final IPCMessage message) {
        
        final String channel = message.getChannel();
        if (channel.equals("SERVER_COMMAND")) {
            
            if (!message.hasNext()) {
                this.logger.log(Level.WARNING, "Incomplete IPC client command sent to this Minecraft server.");
                this.logger.log(Level.WARNING, "Missing sender UUID and command.");
                this.logger.log(Level.WARNING, message.toString());
                return;
            }
            
            final String senderId = message.next();
            final CommandSender sender;
            if (senderId.equals("console")) {
                sender = this.ipcPlugin.getServer().getConsoleSender();
            } else {
                final UUID playerId;
                try {
                    playerId = UUID.fromString(senderId);
                } catch (IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "Unable to decipher command sender UUID.");
                    this.logger.log(Level.WARNING, "Incoming value: " + senderId);
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                sender = this.ipcPlugin.getServer().getPlayer(playerId);
            }
            
            if (sender == null) {
                this.logger.log(Level.WARNING, "Unable to find suitable command sender for the Minecraft server command.");
                this.logger.log(Level.WARNING, "Incoming value: " + senderId);
                return;
            }
            
            if (!message.hasNext()) {
                this.logger.log(Level.WARNING, "Incomplete IPC client command sent to this Minecraft server.");
                this.logger.log(Level.WARNING, "Missing command.");
                this.logger.log(Level.WARNING, message.toString());
                return;
            }
            
            final StringBuilder builder = new StringBuilder();
            builder.append(message.next());
            
            while (message.hasNext()) {
                builder.append(" ");
                builder.append(message.next());
            }
            
            this.ipcPlugin.getServer().dispatchCommand(sender, builder.toString());
        } else {
            this.logger.log(Level.WARNING, "IPCMessage sent to this Minecraft server (name: " + message.getServer() + ").");
            this.logger.log(Level.WARNING, "The channel " + channel + " is not registered to this server.");
            this.logger.log(Level.WARNING, "IPC message data: " + message.toString());
        }
    }
}

/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
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

package org.bspfsystems.bungeeipc.bungeecord;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.jetbrains.annotations.NotNull;

final class BungeeProxyIPCReader implements IPCReader {
    
    private final BungeeIPCPlugin ipcPlugin;
    private final Logger logger;
    
    BungeeProxyIPCReader(@NotNull final BungeeIPCPlugin ipcPlugin) {
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
    }
    
    @Override
    public void readMessage(@NotNull final IPCMessage message) {
        
        final String channel = message.getChannel();
        if (channel.equals("PROXY_COMMAND")) {
            
            if (!message.hasNext()) {
                this.logger.log(Level.WARNING, "Incomplete IPC server command sent to the BungeeCord proxy.");
                this.logger.log(Level.WARNING, "Missing proxy sender UUID, server sender name, and command.");
                this.logger.log(Level.WARNING, message.toString());
                return;
            }
            
            final String proxySenderName = message.next();
            final CommandSender proxySender;
            if (proxySenderName.equals("console")) {
                proxySender = this.ipcPlugin.getProxy().getConsole();
            } else {
                final UUID proxySenderId;
                try {
                    proxySenderId = UUID.fromString(proxySenderName);
                } catch (IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "Unable to decipher proxy command sender UUID.");
                    this.logger.log(Level.WARNING, "Incoming value: " + proxySenderName);
                    this.logger.log(Level.WARNING, "IllegalArgumentException thrown.", e);
                    return;
                }
                proxySender = this.ipcPlugin.getProxy().getPlayer(proxySenderId);
            }
            
            if (proxySender == null) {
                this.logger.log(Level.WARNING, "Unable to find a suitable proxy command sender for the BungeeCord proxy command.");
                this.logger.log(Level.WARNING, "Incoming value: " + proxySenderName);
                return;
            }
            
            if (!message.hasNext()) {
                this.logger.log(Level.WARNING, "Incomplete IPC server command sent to the BungeeCord proxy.");
                this.logger.log(Level.WARNING, "Missing server sender name and command.");
                this.logger.log(Level.WARNING, message.toString());
                proxySender.sendMessage(new ComponentBuilder("Error sending command to proxy: Player name and command not received by proxy.").color(ChatColor.RED).create());
                proxySender.sendMessage(new ComponentBuilder("Please try again. If the issue persists, please contact a server administrator.").color(ChatColor.RED).create());
                return;
            }
            
            final String serverSenderName = message.next();
            final CommandSender serverSender;
            if (serverSenderName.equals("console")) {
                serverSender = this.ipcPlugin.getProxy().getConsole();
            } else {
                ProxiedPlayer serverPlayer = this.ipcPlugin.getProxy().getPlayer(serverSenderName);
                if (serverPlayer == null) {
                    for (final ProxiedPlayer possibleServerPlayer : this.ipcPlugin.getProxy().getPlayers()) {
                        if (possibleServerPlayer.getName().equalsIgnoreCase(serverSenderName)) {
                            serverPlayer = possibleServerPlayer;
                            break;
                        }
                    }
                }
                serverSender = serverPlayer;
            }
            
            if (serverSender == null) {
                this.logger.log(Level.INFO, "No player by name " + serverSenderName + " found online.");
                final ComponentBuilder builder = new ComponentBuilder("Player ").color(ChatColor.RED);
                builder.append(serverSenderName).color(ChatColor.GOLD);
                builder.append(" not found or is not online.").color(ChatColor.RED);
                proxySender.sendMessage(builder.create());
                return;
            }
            
            if (!message.hasNext()) {
                this.logger.log(Level.WARNING, "Incomplete IPC server command sent to the BungeeCord proxy.");
                this.logger.log(Level.WARNING, "Missing command.");
                this.logger.log(Level.WARNING, message.toString());
                proxySender.sendMessage(new ComponentBuilder("Error sending command to proxy: Command not received by proxy.").color(ChatColor.RED).create());
                proxySender.sendMessage(new ComponentBuilder("Please try again. If the issue persists, please contact a server administrator.").color(ChatColor.RED).create());
                return;
            }
            
            final StringBuilder builder = new StringBuilder();
            builder.append(message.next());
            
            while (message.hasNext()) {
                builder.append(" ").append(message.next());
            }
            
            this.ipcPlugin.getProxy().getPluginManager().dispatchCommand(serverSender, builder.toString());
        } else {
            this.logger.log(Level.WARNING, "IPC message sent to the BungeeCord proxy.");
            this.logger.log(Level.WARNING, "The channel " + channel + " is not registered to this BungeeCord proxy.");
            this.logger.log(Level.WARNING, "IPC message data: " + message.toString());
        }
    }
}

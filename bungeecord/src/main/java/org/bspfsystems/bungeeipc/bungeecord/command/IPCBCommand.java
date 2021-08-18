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

package org.bspfsystems.bungeeipc.bungeecord.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.bungeecord.BungeeIPCPlugin;
import org.jetbrains.annotations.NotNull;

public final class IPCBCommand extends Command implements TabExecutor {
    
    private final BungeeIPCPlugin ipcPlugin;
    
    public IPCBCommand(@NotNull final BungeeIPCPlugin ipcPlugin) {
          super("ipcb", "bungeeipc.command.ipcb");
          this.ipcPlugin = ipcPlugin;
    }
    
    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        if (args.length == 0) {
            this.sendSubCommands(sender);
            return;
        }
        
        final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
        final String subCommand = argList.remove(0).toLowerCase();
        
        if (subCommand.equalsIgnoreCase("command")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.command")) {
                sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create());
                return;
            }
            
            if (argList.size() < 3) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb command <server> <sender> <command> [args...]").color(ChatColor.RED).create());
                return;
            }
            
            final String serverName = argList.remove(0);
            final String playerName = argList.remove(0);
            final boolean isConsole;
            
            if (playerName.equals("console")) {
                isConsole = true;
                if (!sender.hasPermission("bungeeipc.command.ipcb.command.player.console")) {
                    sender.sendMessage(new ComponentBuilder("You do not have permission to execute commands as the server console.").color(ChatColor.RED).create());
                    return;
                }
                
            } else if (!playerName.equalsIgnoreCase(sender.getName())) {
                isConsole = false;
                if (!sender.hasPermission("bungeeipc.command.ipcb.command.player.other")) {
                    sender.sendMessage(new ComponentBuilder("You do not have permission to execute commands as another player.").color(ChatColor.RED).create());
                    return;
                }
            } else {
                isConsole = false;
            }
            
            if (!isConsole) {
                ProxiedPlayer player = this.ipcPlugin.getProxy().getPlayer(playerName);
                if (player == null) {
                    for (final ProxiedPlayer possiblePlayer : this.ipcPlugin.getProxy().getPlayers()) {
                        if (possiblePlayer.getName().equalsIgnoreCase(playerName)) {
                            player = possiblePlayer;
                            break;
                        }
                    }
                }
                if (player == null) {
                    final ComponentBuilder builder = new ComponentBuilder("Player ").color(ChatColor.RED);
                    builder.append(playerName).color(ChatColor.GOLD);
                    builder.append(" not found or is not online.").color(ChatColor.RED);
                    sender.sendMessage(builder.create());
                    return;
                }
                if (!player.getServer().getInfo().getName().equals(serverName)) {
                    
                    ComponentBuilder builder = new ComponentBuilder("Player ").color(ChatColor.RED);
                    builder.append(player.getName()).color(ChatColor.GOLD);
                    builder.append(" is not on that server.").color(ChatColor.RED);
                    sender.sendMessage(builder.create());
            
                    builder = new ComponentBuilder("Current server: ").color(ChatColor.AQUA);
                    builder.append(player.getServer().getInfo().getName()).color(ChatColor.GOLD);
                    sender.sendMessage(builder.create());
            
                    builder = new ComponentBuilder("Requested server: ").color(ChatColor.GREEN);
                    builder.append(serverName).color(ChatColor.GOLD);
                    sender.sendMessage(builder.create());
                    return;
                }
            }
            
            final IPCMessage message = new IPCMessage(serverName, "SERVER_COMMAND");
            message.add(playerName);
            for (final String commandPart : argList) {
                message.add(commandPart);
            }
            
            this.ipcPlugin.sendMessage(message);
            
        } else if (subCommand.equalsIgnoreCase("status")) {
    
            if (!sender.hasPermission("bungeeipc.command.ipcb.status")) {
                sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create());
                return;
            }
            
            final boolean isPlayer = sender instanceof ProxiedPlayer;
            if (argList.isEmpty()) {
    
                this.sendStatusHeader(sender, isPlayer);
                final Map<String, ServerInfo> servers = this.ipcPlugin.getProxy().getServers();
                int accessibleServers = 0;
                
                for (final String serverName : servers.keySet()) {
                    if (!servers.get(serverName).canAccess(sender)) {
                        continue;
                    }
                    accessibleServers++;
                    sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append(serverName).color(this.getColor(serverName)).create());
                }
    
                if (accessibleServers == 0) {
                    sender.sendMessage(new ComponentBuilder("No servers.").color(ChatColor.RED).create());
                }
                sender.sendMessage(new ComponentBuilder("================================================================").color(ChatColor.DARK_GRAY).create());
            } else if (argList.size() == 1) {
                
                this.sendStatusHeader(sender, isPlayer);
                final String serverName = argList.remove(0);
                final ServerInfo server = this.ipcPlugin.getProxy().getServerInfo(serverName);
                
                if (server == null) {
                    final ComponentBuilder builder = new ComponentBuilder("Server ").color(ChatColor.RED);
                    builder.append(serverName).color(ChatColor.GOLD);
                    builder.append(" not found.").color(ChatColor.RED);
                    sender.sendMessage(builder.create());
                } else if (!server.canAccess(sender)) {
                    final ComponentBuilder builder = new ComponentBuilder("Server ").color(ChatColor.RED);
                    builder.append(serverName).color(ChatColor.GOLD);
                    builder.append(" not found.").color(ChatColor.RED);
                    sender.sendMessage(builder.create());
                } else {
                    final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                    builder.append(serverName).color(this.getColor(serverName));
                    sender.sendMessage(builder.create());
                }
                sender.sendMessage(new ComponentBuilder("================================================================").color(ChatColor.DARK_GRAY).create());
            } else {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb status [server]").color(ChatColor.RED).create());
            }
            
        } else if (subCommand.equalsIgnoreCase("reconnect")) {
    
            if (!sender.hasPermission("bungeeipc.command.ipcb.reconnect")) {
                sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create());
                return;
            }
            if (argList.size() != 1) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb reconnect <server>").color(ChatColor.RED).create());
                return;
            }
    
            final String serverName = argList.remove(0);
            if (!this.ipcPlugin.isRegisteredServer(serverName)) {
                final ComponentBuilder builder = new ComponentBuilder("Server ").color(ChatColor.RED);
                builder.append(serverName).color(ChatColor.GOLD);
                builder.append(" is not a registered IPC server.").color(ChatColor.RED);
                sender.sendMessage(builder.create());
                return;
            }
            if (!sender.hasPermission("bungeeipc.command.ipcb.reconnect." + serverName.toLowerCase())) {
                final ComponentBuilder builder = new ComponentBuilder("You do not have permission to reconnect IPC server ").color(ChatColor.RED);
                builder.append(serverName).color(ChatColor.GOLD);
                builder.append(".").color(ChatColor.RED);
                sender.sendMessage(builder.create());
                return;
            }
    
            this.ipcPlugin.restartServer(serverName);
            final ComponentBuilder builder = new ComponentBuilder("IPC server ").color(ChatColor.GREEN);
            builder.append(serverName).color(ChatColor.GOLD);
            builder.append(" has been reconnected.").color(ChatColor.GREEN);
            sender.sendMessage(builder.create());
    
        } else if (subCommand.equalsIgnoreCase("reload")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.reload")) {
                sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create());
                return;
            }
            if (argList.size() != 0) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb reload").color(ChatColor.RED).create());
                return;
            }
    
            final ComponentBuilder builder = new ComponentBuilder("Reloading the BungeeIPC configuration. Please run ").color(ChatColor.GOLD);
            builder.append("/ipcb reload").color(ChatColor.AQUA);
            builder.append(" (if possible) in a few seconds to verify that the IPC Servers have reloaded and reconnected successfully.").color(ChatColor.GOLD);
            sender.sendMessage(builder.create());
            this.ipcPlugin.reloadConfig(sender);
        } else {
            this.sendSubCommands(sender);
        }
    }
    
    private void sendSubCommands(@NotNull final CommandSender sender) {
        
        final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
        final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
        final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
        final boolean permissionReload = sender.hasPermission("bungeeipc.command.ipcb.reload");
        
        if (!permissionCommand && !permissionStatus && !permissionReconnect && !permissionReload) {
            sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create());
            return;
        }
        
        sender.sendMessage(new ComponentBuilder("Available commands:").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.DARK_GRAY).create());
        
        if (permissionCommand) {
            final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
            builder.append(" - ").color(ChatColor.WHITE);
            builder.append("/ipcb command <server> <sender> <command> [args...]").color(ChatColor.AQUA);
            sender.sendMessage(builder.create());
        }
        if (permissionStatus) {
            final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
            builder.append(" - ").color(ChatColor.WHITE);
            builder.append("/ipcb status [server]").color(ChatColor.AQUA);
            sender.sendMessage(builder.create());
        }
        if (permissionReconnect) {
            final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
            builder.append(" - ").color(ChatColor.WHITE);
            builder.append("/ipcb reconnect <server>").color(ChatColor.AQUA);
            sender.sendMessage(builder.create());
        }
        if (permissionReload) {
            final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
            builder.append("/ipcb reload").color(ChatColor.AQUA);
            sender.sendMessage(builder.create());
        }
    }
    
    private void sendStatusHeader(@NotNull final CommandSender sender, final boolean isPlayer) {
    
        sender.sendMessage(new ComponentBuilder("================================================================").color(ChatColor.DARK_GRAY).create());
        sender.sendMessage(new ComponentBuilder("Minecraft servers attached to the BungeeCord proxy:").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.DARK_GRAY).create());
        sender.sendMessage(new ComponentBuilder("GRAY").color(ChatColor.GRAY).append("   : No Information").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("RED").color(ChatColor.RED).append("    : Offline").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("BLUE").color(ChatColor.BLUE).append("   : Online, Non-IPC").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("GOLD").color(ChatColor.GOLD).append("   : Online, IPC Not Available").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("YELLOW").color(ChatColor.YELLOW).append(" : Online, IPC Not Connected").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("GREEN").color(ChatColor.GREEN).append("  : Online, IPC Connected").color(ChatColor.WHITE).create());
        sender.sendMessage(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.DARK_GRAY).create());
    
        if (isPlayer) {
            final String serverName = ((ProxiedPlayer) sender).getServer().getInfo().getName();
            sender.sendMessage(new ComponentBuilder("Current Server: ").color(ChatColor.WHITE).append(serverName).color(this.getColor(serverName)).create());
            sender.sendMessage(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.DARK_GRAY).create());
        }
    }
    
    @NotNull
    private ChatColor getColor(@NotNull final String serverName) {
        
        final int onlineStatus = this.ipcPlugin.getOnlineStatus(serverName);
        final boolean registered = this.ipcPlugin.isRegisteredServer(serverName);
        final boolean available = this.ipcPlugin.isServerRunning(serverName);
        final boolean connected = this.ipcPlugin.isServerConnected(serverName);
        
        if (onlineStatus == 1) {
            if (!registered) {
                return ChatColor.BLUE;
            } else if (connected) {
                return ChatColor.GREEN;
            } else if (available) {
                return ChatColor.YELLOW;
            } else {
                return ChatColor.GOLD;
            }
        } else if (onlineStatus == 0) {
            return ChatColor.RED;
        } else {
            return ChatColor.GRAY;
        }
    }
    
    @NotNull
    @Override
    public Iterable<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
        final ArrayList<String> completions = new ArrayList<String>();
        
        final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
        final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
        final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
        final boolean permissionReload = sender.hasPermission("bungeeipc.command.ipcb.reload");
        
        if (permissionCommand) {
            completions.add("command");
        }
        if (permissionStatus) {
            completions.add("status");
        }
        if (permissionReconnect) {
            completions.add("reconnect");
        }
        if (permissionReload) {
            completions.add("reload");
        }
        
        if (argList.isEmpty()) {
            return completions;
        }
        
        final String subCommand = argList.remove(0);
        if (argList.isEmpty()) {
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(subCommand.toLowerCase()));
            return completions;
        }
        
        completions.clear();
        if (!permissionCommand && !permissionStatus && !permissionReconnect && !permissionReload) {
            return completions;
        }
        if (subCommand.equalsIgnoreCase("reload")) {
            return completions;
        }
        
        for (final ServerInfo server : this.ipcPlugin.getProxy().getServers().values()) {
            if (server.canAccess(sender)) {
                completions.add(server.getName());
            }
        }
        
        if (argList.isEmpty()) {
            return completions;
        }
        
        final String serverName = argList.remove(0);
        if (argList.isEmpty()) {
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(serverName.toLowerCase()));
            return completions;
        }
        
        completions.clear();
        
        if (permissionCommand && subCommand.equalsIgnoreCase("command")) {
            
            for (final ProxiedPlayer player : this.ipcPlugin.getProxy().getPlayers()) {
                completions.add(player.getName());
            }
            
            if (argList.isEmpty()) {
                return completions;
            }
            
            final String playerName = argList.remove(0);
            if (argList.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(playerName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            return completions;
        } else if (permissionStatus && subCommand.equalsIgnoreCase("status")) {
            return completions;
        } else if (permissionReconnect && subCommand.equalsIgnoreCase("reconnect")) {
            return completions;
        } else {
            return completions;
        }
    }
}

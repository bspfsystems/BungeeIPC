/* 
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
 * 
 * Copyright (C) 2020-2022 BSPF Systems, LLC (https://bspfsystems.org/)
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
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.server.ServerIPCMessage;
import org.bspfsystems.bungeeipc.bungeecord.BungeeIPCPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the implementation of the {@code /ipcb} {@link Command} and
 * {@link TabExecutor} for tab-completion.
 */
public final class IPCBCommand extends Command implements TabExecutor {
    
    private final BungeeIPCPlugin ipcPlugin;
    
    /**
     * Constructs a new {@link IPCBCommand}.
     * 
     * @param ipcPlugin The {@link BungeeIPCPlugin}.
     * @see Command#Command(String, String, String...)
     */
    public IPCBCommand(@NotNull final BungeeIPCPlugin ipcPlugin) {
          super("ipcb", "bungeeipc.command.ipcb");
          this.setPermissionMessage("§r§cYou do not have permission to execute this command!§r");
          this.ipcPlugin = ipcPlugin;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if (argsList.isEmpty()) {
            if (sender.hasPermission("bungeeipc.command.ipcb.help")) {
                sender.sendMessage(TextComponent.fromLegacyText("§r§cIncomplete BungeeIPC command. Please use§r §b/ipcb help§r §cfor help.§r"));
            } else {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
            }
            return;
        }
        
        final String subCommand = argsList.remove(0);
        if (subCommand.equalsIgnoreCase("command")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.command")) {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            
            if (argsList.size() < 3) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb command <server> <sender> <command> [args...]").color(ChatColor.RED).create());
                return;
            }
            
            final String serverName = argsList.remove(0);
            final String playerName = argsList.remove(0);
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
            
            final IPCMessage message = new ServerIPCMessage(serverName, "SERVER_COMMAND");
            message.add(playerName);
            for (final String commandPart : argsList) {
                message.add(commandPart);
            }
            
            this.ipcPlugin.sendMessage(message);
            
        } else if (subCommand.equalsIgnoreCase("help")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.help")) {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            if (!argsList.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb help").color(ChatColor.RED).create());
                return;
            }
            
            final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
            final boolean permissionHelp = sender.hasPermission("bungeeipc.command.ipcb.help");
            final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
            final boolean permissionReload = sender.hasPermission("bungeeipc.command.ipcb.reload");
            final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
            
            if (!permissionCommand && !permissionHelp && !permissionReconnect && !permissionReload && !permissionStatus) {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            
            sender.sendMessage(new ComponentBuilder("Available commands:").color(ChatColor.GOLD).create());
            sender.sendMessage(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.DARK_GRAY).create());
            
            if (permissionCommand) {
                final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                builder.append("/ipcb command <server> <sender> <command> [args...]").color(ChatColor.AQUA);
                sender.sendMessage(builder.create());
            }
            if (permissionHelp) {
                final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                builder.append("/ipcb help").color(ChatColor.AQUA);
                sender.sendMessage(builder.create());
            }
            if (permissionReconnect) {
                final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                builder.append("/ipcb reconnect <server>").color(ChatColor.AQUA);
                sender.sendMessage(builder.create());
            }
            if (permissionReload) {
                final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                builder.append("/ipcb reload").color(ChatColor.AQUA);
                sender.sendMessage(builder.create());
            }
            if (permissionStatus) {
                final ComponentBuilder builder = new ComponentBuilder(" - ").color(ChatColor.WHITE);
                builder.append("/ipcb status [server]").color(ChatColor.AQUA);
                sender.sendMessage(builder.create());
            }
            
        } else if (subCommand.equalsIgnoreCase("reconnect")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.reconnect")) {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            if (argsList.size() != 1) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb reconnect <server>").color(ChatColor.RED).create());
                return;
            }
            
            final String serverName = argsList.remove(0);
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
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            if (!argsList.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("Syntax: /ipcb reload").color(ChatColor.RED).create());
                return;
            }
            
            final ComponentBuilder builder = new ComponentBuilder("Reloading the BungeeIPC configuration. Please run ").color(ChatColor.GOLD);
            builder.append("/ipcb reload").color(ChatColor.AQUA);
            builder.append(" (if possible) in a few seconds to verify that the IPC Servers have reloaded and reconnected successfully.").color(ChatColor.GOLD);
            sender.sendMessage(builder.create());
            this.ipcPlugin.reloadConfig(sender);
            
        } else if (subCommand.equalsIgnoreCase("status")) {
            
            if (!sender.hasPermission("bungeeipc.command.ipcb.status")) {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
                return;
            }
            
            final boolean isPlayer = sender instanceof ProxiedPlayer;
            if (argsList.isEmpty()) {
                
                this.sendStatusHeader(sender, isPlayer);
                int accessibleServers = 0;
                for (final ServerInfo server : this.ipcPlugin.getProxy().getServers().values()) {
                    if (!server.canAccess(sender)) {
                        continue;
                    }
                    accessibleServers++;
                    sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append(server.getName()).color(this.getColor(server.getName())).create());
                }
                
                if (accessibleServers == 0) {
                    sender.sendMessage(new ComponentBuilder("No servers.").color(ChatColor.RED).create());
                }
                sender.sendMessage(new ComponentBuilder("================================================================").color(ChatColor.DARK_GRAY).create());
            } else if (argsList.size() == 1) {
                
                this.sendStatusHeader(sender, isPlayer);
                final String serverName = argsList.remove(0);
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
            
        } else {
            if (sender.hasPermission("bungeeipc.command.ipcb.help")) {
                sender.sendMessage(TextComponent.fromLegacyText("§r§cIncomplete BungeeIPC command. Please use§r §b/ipcb help§r §cfor help.§r"));
            } else {
                sender.sendMessage(TextComponent.fromLegacyText(this.getPermissionMessage()));
            }
        }
    }
    
    /**
     * Sends a set of header messages to the given {@link CommandSender},
     * specifying the various colors of servers and their meanings.
     * 
     * @param sender The {@link CommandSender} asking for the header.
     * @param isPlayer {@code true} if the given {@link CommandSender} is a
     *                 {@link ProxiedPlayer}, {@code false} otherwise.
     */
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
    
    /**
     * Gets the {@link ChatColor} that corresponds to the status of the
     * {@link Server} with the given name.
     * 
     * @param serverName The name of the {@link Server} for which to retrieve
     *                   the status {@link ChatColor}.
     * @return The corresponding status {@link ChatColor}.
     */
    @NotNull
    private ChatColor getColor(@NotNull final String serverName) {
        
        final int onlineStatus = this.ipcPlugin.getOnlineStatus(serverName);
        
        if (onlineStatus == 1) {
            if (!this.ipcPlugin.isRegisteredServer(serverName)) {
                return ChatColor.BLUE;
            } else if (this.ipcPlugin.isServerConnected(serverName)) {
                return ChatColor.GREEN;
            } else if (this.ipcPlugin.isServerRunning(serverName)) {
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Iterable<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        final List<String> completions = new ArrayList<String>();
        
        final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
        final boolean permissionHelp = sender.hasPermission("bungeeipc.command.ipcb.help");
        final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
        final boolean permissionReload = sender.hasPermission("bungeeipc.command.ipcb.reload");
        final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
        
        if (permissionCommand) {
            completions.add("command");
        }
        if (permissionHelp) {
            completions.add("help");
        }
        if (permissionReconnect) {
            completions.add("reconnect");
        }
        if (permissionReload) {
            completions.add("reload");
        }
        if (permissionStatus) {
            completions.add("status");
        }
        
        if (argsList.isEmpty()) {
            return completions;
        }
        
        final String subCommand = argsList.remove(0);
        if (argsList.isEmpty()) {
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(subCommand.toLowerCase()));
            return completions;
        }
        
        completions.clear();
        if (subCommand.equalsIgnoreCase("command")) {
            
            if (!permissionCommand) {
                return completions;
            }
            
            for (final ServerInfo server : this.ipcPlugin.getProxy().getServers().values()) {
                if (server.canAccess(sender)) {
                    completions.add(server.getName());
                }
            }
            
            final String serverName = argsList.remove(0);
            if (argsList.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(serverName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            final ServerInfo server = this.ipcPlugin.getProxy().getServerInfo(serverName);
            if (server == null || !server.canAccess(sender)) {
                return completions;
            }
            
            for (final ProxiedPlayer player : server.getPlayers()) {
                if (player.getName().equalsIgnoreCase(sender.getName())) {
                    completions.add(player.getName());
                } else if (sender.hasPermission("bungeeipc.command.ipcb.command.player.other")) {
                    completions.add(player.getName());
                }
            }
            
            if (sender.hasPermission("bungeeipc.command.ipcb.command.player.console")) {
                completions.add("console");
            }
            
            final String playerName = argsList.remove(0);
            if (argsList.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(playerName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("help")) {
            return completions;
        } else if (subCommand.equalsIgnoreCase("reconnect")) {
            
            if (!permissionReconnect) {
                return completions;
            }
            
            for (final ServerInfo server : this.ipcPlugin.getProxy().getServers().values()) {
                if (server.canAccess(sender)) {
                    completions.add(server.getName());
                }
            }
            
            final String serverName = argsList.remove(0);
            if (argsList.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(serverName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("reload")) {
            return completions;
        } else if (subCommand.equalsIgnoreCase("status")) {
            
            if (!permissionStatus) {
                return completions;
            }
            
            for (final ServerInfo server : this.ipcPlugin.getProxy().getServers().values()) {
                if (server.canAccess(sender)) {
                    completions.add(server.getName());
                }
            }
            
            final String serverName = argsList.remove(0);
            if (argsList.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(serverName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            return completions;
            
        } else {
            return completions;
        }
    }
}

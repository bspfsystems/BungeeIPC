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
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bspfsystems.bungeeipc.api.common.IPCSocket;
import org.bspfsystems.bungeeipc.api.server.ServerIPCSocket;
import org.bspfsystems.bungeeipc.bungeecord.BungeeIPCPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the implementation of the {@code /server} {@link Command} and
 * {@link TabExecutor} for tab-completion.
 * <p>
 * This {@link Command} replaces the default BungeeCord {@code /server}
 * {@link Command} with a more visually-appealing one that retains the same
 * functionality, as well as expanding on it with compatibility for
 * {@link BungeeIPCPlugin} functions.
 */
public final class ServerCommand extends Command implements TabExecutor {
    
    private final BungeeIPCPlugin ipcPlugin;
    
    /**
     * Constructs a new {@link ServerCommand}.
     * 
     * @param ipcPlugin The {@link BungeeIPCPlugin}.
     */
    public ServerCommand(@NotNull final BungeeIPCPlugin ipcPlugin) {
        super("server", "bungeeipc.command.server");
        this.ipcPlugin = ipcPlugin;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        final boolean isPlayer = sender instanceof ProxiedPlayer;
        if (args.length == 0) {
            this.listServers(sender, isPlayer);
        } else if (args.length == 1) {
            if (!isPlayer) {
                this.sendSyntax(sender, isPlayer);
            } else {
                this.changeServers((ProxiedPlayer) sender, args[0]);
            }
        } else {
            this.sendSyntax(sender, isPlayer);
        }
    }
    
    /**
     * Sends the syntax for this {@link Command} to the given
     * {@link CommandSender}. There is a difference in the syntax sent based on
     * whether the {@link CommandSender} is a {@link ProxiedPlayer} or not.
     * 
     * @param sender The {@link CommandSender} executing this {@link Command}.
     * @param isPlayer {@code true} if the {@link CommandSender} is a
     *                 {@link ProxiedPlayer}, {@code false} otherwise.
     */
    private void sendSyntax(@NotNull final CommandSender sender, final boolean isPlayer) {
        
        final ComponentBuilder builder = new ComponentBuilder("Syntax: /server").color(ChatColor.RED);
        if (isPlayer) {
            builder.append(" [server name]");
        } else {
            builder.append("");
        }
        builder.color(ChatColor.RED);
        sender.sendMessage(builder.create());
    }
    
    /**
     * Lists the {@link ServerInfo} names and their connection statuses to the
     * given {@link CommandSender}. Additionally, if the {@link CommandSender}
     * is a {@link ProxiedPlayer}, the {@link ServerInfo Server} that they are
     * connected to will be displayed.
     *
     * @param sender The {@link CommandSender} executing this {@link Command}.
     * @param isPlayer {@code true} if the {@link CommandSender} is a
     *                 {@link ProxiedPlayer}, {@code false} otherwise.
     */
    private void listServers(@NotNull final CommandSender sender, final boolean isPlayer) {
        
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
    }
    
    /**
     * Gets the {@link ChatColor} used to color the {@link ServerInfo Server}
     * name for status information. The {@link ChatColor}s used are listed as
     * follows:
     * <li>{@link ChatColor#GRAY} - No information</li>
     * <li>{@link ChatColor#RED} - Minecraft server disconnected/offline</li>
     * <li>{@link ChatColor#BLUE} - Minecraft server online, not an
     *     IPC-enabled server</li>
     * <li>{@link ChatColor#GOLD} - Minecraft server online, IPC-enabled, not
     *     yet available (the {@link ServerIPCSocket} has not started.</li>
     * <li>{@link ChatColor#YELLOW} - Minecraft server online, IPC-enabled, not
     *     yet connected (the opposing {@link IPCSocket} has not yet
     *     connected).</li>
     * <li>{@link ChatColor#GREEN} - Minecraft server online, IPC-enabled, IPC
     *     has connected.</li>
     * 
     * @param serverName The name of the {@link ServerInfo Server} to get the
     *                   status color of.
     * @return The {@link ChatColor} associated with the status of the server.
     */
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
    
    /**
     * Connects the {@link ProxiedPlayer} to the {@link ServerInfo Server} with
     * the given name, if it exists, and the {@link ProxiedPlayer} has
     * permission to access it.
     * 
     * @param player The {@link ProxiedPlayer} attempting to change servers.
     * @param serverName The name of the {@link ServerInfo Server} to switch the
     *                   {@link ProxiedPlayer} to.
     */
    private void changeServers(@NotNull final ProxiedPlayer player, @NotNull final String serverName) {
        
        final ServerInfo server = this.ipcPlugin.getProxy().getServerInfo(serverName);
        if (server == null) {
            final ComponentBuilder builder = new ComponentBuilder("Server ").color(ChatColor.RED);
            builder.append(serverName).color(ChatColor.GOLD);
            builder.append(" not found.").color(ChatColor.RED);
            player.sendMessage(builder.create());
        } else if (!server.canAccess(player)) {
            player.sendMessage(new ComponentBuilder("YOu do not have permission to access that server.").color(ChatColor.RED).create());
        } else {
            player.connect(server, ServerConnectEvent.Reason.COMMAND);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Iterable<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        if (!(sender instanceof ProxiedPlayer)) {
            return Collections.emptyList();
        }
        
        final List<String> argList = new ArrayList<String>(Arrays.asList(args));
        final List<String> completions = new ArrayList<String>();
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
        return completions;
    }
}

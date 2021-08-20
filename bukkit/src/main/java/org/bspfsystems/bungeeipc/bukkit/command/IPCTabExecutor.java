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

package org.bspfsystems.bungeeipc.bukkit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.bukkit.BukkitIPCPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the implementation of the {@link TabExecutor} interface for the
 * {@code /ipc} command execution and tab-completion.
 */
public final class IPCTabExecutor implements TabExecutor {
    
    private final BukkitIPCPlugin ipcPlugin;
    private final Logger logger;
    
    /**
     * Constructs the {@link IPCTabExecutor}.
     * 
     * @param ipcPlugin The {@link BukkitIPCPlugin}.
     */
    public IPCTabExecutor(@NotNull final BukkitIPCPlugin ipcPlugin) {
        this.ipcPlugin = ipcPlugin;
        this.logger = this.ipcPlugin.getLogger();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        
        final String commandName = command.getName();
        if (!this.ipcPlugin.getDescription().getCommands().containsKey(commandName)) {
            this.logger.log(Level.WARNING, "The command " + commandName + " was triggered in the BungeeIPC plugin.");
            this.logger.log(Level.WARNING, "This command is not registered to BungeeIPC.");
            return true;
        }
        
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        
        if (commandName.equalsIgnoreCase("ipc")) {
            
            if (argsList.isEmpty()) {
                if (sender.hasPermission("bungeeipc.command.ipc.help")) {
                    return false;
                }
                sender.sendMessage(this.getPermissionMessage(command));
                return true;
            }
    
            final String subCommand = argsList.remove(0);
            if (subCommand.equalsIgnoreCase("command")) {
        
                if (!sender.hasPermission("bungeeipc.command.ipc.command")) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
                if (argsList.size() < 2) {
                    sender.sendMessage("§r§cSyntax: /ipc command <player> <command> [args...]§r");
                    return true;
                }
        
                final String playerName = argsList.remove(0);
                if (playerName.equalsIgnoreCase("console")) {
                    if (!sender.hasPermission("bungeeipc.command.ipc.command.player.console")) {
                        sender.sendMessage("§r§cYou do not have permission to send commands to the proxy as the proxy console.§r");
                        return true;
                    }
                } else if (!playerName.equalsIgnoreCase(sender.getName())) {
                    if (!sender.hasPermission("bungeeipc.command.ipc.command.player.other")) {
                        sender.sendMessage("§r§cYou do not have permission to send commands to the proxy as another player.§r");
                        return true;
                    }
                }
        
                final IPCMessage message = new IPCMessage("proxy", "PROXY_COMMAND");
                if (sender instanceof Player) {
                    message.add(((Player) sender).getUniqueId().toString());
                } else {
                    message.add("console");
                }
                message.add(playerName);
        
                for (final String commandPart : argsList) {
                    message.add(commandPart);
                }
        
                this.ipcPlugin.sendMessage(message);
                return true;
        
            } else if (subCommand.equalsIgnoreCase("help")) {
    
                if (!sender.hasPermission("bungeeipc.command.ipc.help")) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
                if (!argsList.isEmpty()) {
                    sender.sendMessage("§r§cSyntax: /ipc help§r");
                    return true;
                }
    
                final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipc.command");
                final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipc.status");
                final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipc.reconnect");
                final boolean permissionReload = sender.hasPermission("bungeeipc.command.ipc.reload");
                final boolean permissionHelp = sender.hasPermission("bungeeipc.command.ipc.help");
    
                if (!permissionCommand && !permissionStatus && !permissionReconnect && !permissionReload && !permissionHelp) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
    
                sender.sendMessage("§r§6Available commands:§r");
                sender.sendMessage("§r§8----------------------------------------------------------------§8");
    
                if (permissionCommand) {
                    sender.sendMessage("§r §f-§r §b/ipc command <sender> <command> [args...]§r");
                }
                if (permissionStatus) {
                    sender.sendMessage("§r §f-§r §b/ipc status§r");
                }
                if (permissionReconnect) {
                    sender.sendMessage("§r §f-§r §b/ipc reconnect§r");
                }
                if (permissionReload) {
                    sender.sendMessage("§r §f-§r §b/ipc reload§r");
                }
                if (permissionHelp) {
                    sender.sendMessage("§r §f-§r §b/ipc help§r");
                }
    
                return true;
    
            } else if (subCommand.equalsIgnoreCase("reconnect")) {
    
                if (!sender.hasPermission("bungeeipc.command.ipc.reconnect")) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
                if (!argsList.isEmpty()) {
                    sender.sendMessage("§r§cSyntax: /ipc reconnect§r");
                    return true;
                }
    
                sender.sendMessage("§r§bRestarting the IPC Client connection. Please run /ipc status (if possible) in a few seconds to verify that the reconnect finished successfully.§r");
                this.ipcPlugin.restartClient();
                return true;
    
            } else if (subCommand.equalsIgnoreCase("reload")) {
    
                if (!sender.hasPermission("bungeeipc.command.ipc.reload")) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
                if (!argsList.isEmpty()) {
                    sender.sendMessage("§r§cSyntax: /ipc reload§r");
                    return true;
                }
    
                sender.sendMessage("§r§6Reloading the BungeeIPC configuration. Please run§r §b/ipc status§r §6(if possible) in a few seconds to verify that the IPC Client has reloaded and reconnected finished successfully.§r");
                this.ipcPlugin.reloadConfig(sender);
                return true;
    
            } else if (subCommand.equalsIgnoreCase("status")) {
        
                if (!sender.hasPermission("bungeeipc.command.ipc.status")) {
                    sender.sendMessage(this.getPermissionMessage(command));
                    return true;
                }
                if (!argsList.isEmpty()) {
                    sender.sendMessage("§r§cSyntax: /ipc status§r");
                    return true;
                }
        
                sender.sendMessage("§r§8================================================================§r");
                sender.sendMessage("§r§fIPC Client Status§r");
                sender.sendMessage("§r§8----------------------------------------------------------------§r");
        
                if (this.ipcPlugin.isClientConnected()) {
                    sender.sendMessage("§r§aConnection Completed§r");
                } else if (this.ipcPlugin.isClientRunning()) {
                    sender.sendMessage("§r§6Connection Available§r");
                } else {
                    sender.sendMessage("§r§cNot Connected§r");
                }
        
                sender.sendMessage("§r§8================================================================§r");
                return true;
        
            } else {
                if (sender.hasPermission("bungeeipc.command.ipc.help")) {
                    return false;
                }
                sender.sendMessage(this.getPermissionMessage(command));
                return true;
            }
            
        } else {
            this.logger.log(Level.WARNING, "The command " + commandName + " was triggered in the BungeeIPC plugin.");
            this.logger.log(Level.WARNING, "This command is not registered to BungeeIPC.");
            this.logger.log(Level.WARNING, "This command passed the earlier check undetected.");
            return true;
        }
    }
    
    /**
     * Gets the no-permission message to send based on the given
     * {@link Command}.
     * <p>
     * If the {@link Command}'s permission message is {@code null}, the default
     * no-permission message will be returned..
     * 
     * @param command The {@link Command} to get the permission message from (if
     *                any).
     * @return The no-permission message of the {@link Command}, or a default
     *         one if it is {@code null}.
     */
    @NotNull
    private String getPermissionMessage(@NotNull final Command command) {
        return command.getPermissionMessage() != null ? command.getPermissionMessage() : "§r§cI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.§r";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
    
        final String commandName = command.getName();
        if (!this.ipcPlugin.getDescription().getCommands().containsKey(commandName)) {
            this.logger.log(Level.WARNING, "The command " + commandName + " was triggered for tab-completion in the BungeeIPC plugin.");
            this.logger.log(Level.WARNING, "This command is not registered to BungeeIPC.");
            return Collections.emptyList();
        }
        
        if (commandName.equalsIgnoreCase("ipc")) {
    
            final List<String> argList = new ArrayList<String>(Arrays.asList(args));
            final List<String> completions = new ArrayList<String>();
    
            if (sender.hasPermission("bungeeipc.command.ipc.command")) {
                completions.add("command");
            }
            if (sender.hasPermission("bungeeipc.command.ipc.help")) {
                completions.add("help");
            }
            if (sender.hasPermission("bungeeipc.command.ipc.reconnect")) {
                completions.add("reconnect");
            }
            if (sender.hasPermission("bungeeipc.command.ipc.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("bungeeipc.command.ipc.status")) {
                completions.add("status");
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
            return completions;
            
        } else {
            this.logger.log(Level.WARNING, "The command " + commandName + " was triggered for tab-completion in the BungeeIPC plugin.");
            this.logger.log(Level.WARNING, "This command is not registered to BungeeIPC.");
            this.logger.log(Level.WARNING, "This command passed the earlier check undetected.");
            return Collections.emptyList();
        }
    }
}

/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
 * 
 * Copyright (C) 2020  Matt Ciolkosz (https://github.com/mciolkosz)
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

package org.bspfsystems.bungeeipc.bungeebukkit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bspfsystems.bungeeipc.bungeebukkit.BungeeIPCPlugin;
import org.bspfsystems.bungeeipc.bungeebukkit.api.IPCMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class IPCTabExecutor implements TabExecutor {
	
	private final BungeeIPCPlugin ipcPlugin;
	
	public IPCTabExecutor(final BungeeIPCPlugin ipcPlugin) {
		this.ipcPlugin = ipcPlugin;
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		
		if(args.length == 0) {
			return sendSubCommands(sender, command.getPermissionMessage());
		}
		
		final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
		final String subCommand = argList.remove(0);
		
		if(subCommand.equals("command")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipc.command")) {
				sender.sendMessage(command.getPermissionMessage());
				return true;
			}
			
			if(argList.size() < 2) {
				sender.sendMessage("§r§cSyntax: /ipc command <player> <command> [args...]§r");
				return true;
			}
			
			final String playerName = argList.remove(0);
			if(playerName.equalsIgnoreCase("console")) {
				if(!sender.hasPermission("bungeeipc.command.ipc.command.player.console")) {
					sender.sendMessage("§r§cYou do not have permission to send commands to the proxy as the proxy console.§r");
					return true;
				}
			}
			else if(!playerName.equalsIgnoreCase(sender.getName())) {
				if(!sender.hasPermission("bungeeipc.command.ipc.command.player.other")) {
					sender.sendMessage("§r§cYou do not have permission to send commands to the proxy as another player.§r");
					return true;
				}
			}
			
			final IPCMessage ipcMessage = new IPCMessage("PROXY_COMMAND");
			if(sender instanceof Player) {
				ipcMessage.add(((Player) sender).getUniqueId().toString());
			}
			else {
				ipcMessage.add("console");
			}
			ipcMessage.add(playerName);
			for(final String commandPart : argList) {
				ipcMessage.add(commandPart);
			}
			
			ipcPlugin.sendMessage(ipcMessage);
			return true;
		}
		else if(subCommand.equals("status")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipc.status")) {
				sender.sendMessage(command.getPermissionMessage());
				return true;
			}
			
			if(argList.size() == 0) {
				sender.sendMessage("§r§8================================================§r");
				sender.sendMessage("§r§fIPC Server Status§r");
				sender.sendMessage("§r§8------------------------------------------------§r");
				
				if(ipcPlugin.isClientConnected()) {
					sender.sendMessage("§r§aConnection Online§r");
				}
				else if(ipcPlugin.isClientAvailable()) {
					sender.sendMessage("§r§6Connection Available§r");
				}
				else {
					sender.sendMessage("§r§cNot Connected§r");
				}
				
				sender.sendMessage("§r§8================================================§r");
			}
			else {
				sender.sendMessage("§r§cSyntax: /ipc status");
			}
			
			return true;
		}
		else if(subCommand.equals("reconnect")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipc.reconnect")) {
				sender.sendMessage(command.getPermissionMessage());
				return true;
			}
			
			if(argList.size() == 0) {
				sender.sendMessage("§r§bRestarting IPC connection. Please run /ipc status (if possible) in a few seconds to verify that reconnection occurred.§r");
				ipcPlugin.restartClient();
				return true;
			}
			else {
				sender.sendMessage("§r§cSyntax: /ipc reconnect§r");
			}
			
			return true;
		}
		else {
			return sendSubCommands(sender, command.getPermissionMessage());
		}
	}
	
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
		
		final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
		final ArrayList<String> completions = new ArrayList<String>();
		
		if(sender.hasPermission("bungeeipc.command.ipc.command")) {
			completions.add("command");
		}
		if(sender.hasPermission("bungeeipc.command.ipc.status")) {
			completions.add("status");
		}
		if(sender.hasPermission("bungeeipc.command.ipc.reconnect")) {
			completions.add("reconnect");
		}
		
		if(argList.isEmpty()) {
			return completions;
		}
		
		final String subCommand = argList.remove(0);
		if(argList.isEmpty()) {
			
			final Iterator<String> iterator = completions.iterator();
			while(iterator.hasNext()) {
				if(!iterator.next().toLowerCase().startsWith(subCommand.toLowerCase())) {
					iterator.remove();
				}
			}
			
			return completions;
		}
		
		completions.clear();
		return completions;
	}
	
	private boolean sendSubCommands(final CommandSender sender, final String permissionMessage) {
		
		final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipc.command");
		final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipc.status");
		final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipc.reconnect");
		
		if(!permissionCommand && !permissionStatus && !permissionReconnect) {
			sender.sendMessage(permissionMessage);
			return true;
		}
		
		sender.sendMessage("§r§6Available commands:§r");
		sender.sendMessage("§r§8------------------------------------------------§r");
		
		if(permissionCommand) {
			sender.sendMessage("§r §f-§r §b/ipcb command <server> <sender> <command> [args]§r");
		}
		if(permissionStatus) {
			sender.sendMessage("§r §f-§r §b/ipcb status§r");
		}
		if(permissionReconnect) {
			sender.sendMessage("§r §f-§r §b/ipcb reconnect§r");
		}
		return true;
	}
}

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

package org.bspfsystems.bungeeipc.bungeeproxy.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.bspfsystems.bungeeipc.bungeeproxy.BungeeIPCPlugin;
import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCMessage;
import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCServer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class IPCBCommand extends Command implements TabExecutor {
	
	private final BungeeIPCPlugin ipcPlugin;
	
	public IPCBCommand(final BungeeIPCPlugin ipcPlugin) {
		
		super("ipcb", "bungeeipc.command.ipcb");
		
		this.ipcPlugin = ipcPlugin;
	}
	
	@Override
	public void execute(final CommandSender sender, final String[] args) {
		
		if(args.length == 0) {
			sendSubCommands(sender);
			return;
		}
		
		final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
		final String subCommand = argList.remove(0).toLowerCase();
		
		if(subCommand.equals("command")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipcb.command")) {
				sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
				return;
			}
			
			if(argList.size() < 3) {
				sender.sendMessage(new ComponentBuilder("Syntax: /ipcb command <server> <player> <command> [args...]").color(ChatColor.RED).create());
				return;
			}
			
			final String serverName = argList.remove(0);
			final String playerName = argList.remove(0);
			final boolean isConsole;
			
			if(playerName.equalsIgnoreCase("console")) {
				isConsole = true;
				if(!sender.hasPermission("bungeeipc.command.ipcb.command.player.console")) {
					sender.sendMessage(new ComponentBuilder("You do not have permission to execute commands as the server console.").color(ChatColor.RED).create());
					return;
				}
			}
			else if(!playerName.equalsIgnoreCase(sender.getName())) {
				isConsole = false;
				if(!sender.hasPermission("bungeeipc.command.ipcb.command.player.other")) {
					sender.sendMessage(new ComponentBuilder("You do not have permission to execute commands.").color(ChatColor.RED).create());
					return;
				}
			}
			else {
				isConsole = false;
			}
			
			if(!isConsole) {
				final ProxiedPlayer player = ipcPlugin.getProxy().getPlayer(playerName);
				if(player == null) {
					
					final ComponentBuilder builder = new ComponentBuilder("Player ").color(ChatColor.RED);
					builder.append(playerName).color(ChatColor.GOLD);
					builder.append(" is not online.").color(ChatColor.RED);
					sender.sendMessage(builder.create());
					return;
				}
				if(!player.getServer().getInfo().getName().equals(serverName)) {
					
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
			
			final IPCMessage ipcMessage = new IPCMessage(serverName, "SERVER_COMMAND");
			ipcMessage.add(playerName);
			for(final String commandPart : argList) {
				ipcMessage.add(commandPart);
			}
			
			ipcPlugin.sendMessage(ipcMessage);
		}
		else if(subCommand.equals("status")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipcb.status")) {
				sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
				return;
			}
			
			final boolean isPlayer = sender instanceof ProxiedPlayer;
			if(argList.size() == 0) {
				listServers(sender, isPlayer, false);
			}
			else if(argList.size() == 1) {
				if(argList.get(0).equals("-i") || argList.get(0).equals("--info")) {
					listServers(sender, isPlayer, true);
				}
				else {
					sendSyntax(sender, isPlayer);
				}
			}
			else {
				sendSyntax(sender, isPlayer);
			}
		}
		else if(subCommand.equals("reconnect")) {
			
			if(!sender.hasPermission("bungeeipc.command.ipcb.reconnect")) {
				sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
				return;
			}
			
			if(argList.size() != 1) {
				sender.sendMessage(new ComponentBuilder("Syntax: /ipcb reconnect <server>").color(ChatColor.RED).create());
				return;
			}
			
			final String serverName = argList.get(0);
			if(!ipcPlugin.isRegisteredServer(serverName)) {
				sender.sendMessage(new ComponentBuilder("Server ").color(ChatColor.RED).append(serverName).color(ChatColor.GOLD).append(" is not a registered IPC server.").color(ChatColor.RED).create());
				return;
			}
			
			if(!sender.hasPermission("bungeeipc.command.ipcb.reconnect." + serverName.toLowerCase())) {
				sender.sendMessage(new ComponentBuilder("You do not have permission to reconnect IPC server ").color(ChatColor.RED).append(serverName).color(ChatColor.GOLD).append(".").color(ChatColor.RED).create());
				return;
			}
			
			ipcPlugin.restartServer(serverName);
			sender.sendMessage(new ComponentBuilder("Server ").color(ChatColor.GREEN).append(serverName).color(ChatColor.GOLD).append(" has been reconnected.").color(ChatColor.GREEN).create());
		}
		else {
			sendSubCommands(sender);
		}
	}
	
	private void sendSubCommands(final CommandSender sender) {
		
		final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
		final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
		final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
		
		if(!permissionCommand && !permissionStatus && !permissionReconnect) {
			sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
			return;
		}
		
		sender.sendMessage(new ComponentBuilder("Available commands:").color(ChatColor.GOLD).create());
		sender.sendMessage(new ComponentBuilder("------------------------------------------------").color(ChatColor.DARK_GRAY).create());
		
		if(permissionCommand) {
			sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append("/ipcb command <server> <sender> <command> [args]").color(ChatColor.AQUA).create());
		}
		if(permissionStatus) {
			sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append("/ipcb status [server]").color(ChatColor.AQUA).create());
		}
		if(permissionReconnect) {
			sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append("/ipcb reconnect <server>").color(ChatColor.AQUA).create());
		}
		return;
	}
	
	private void sendSyntax(final CommandSender sender, final boolean isPlayer) {
		
		final ComponentBuilder builder = new ComponentBuilder("Syntax: /server [").color(ChatColor.RED);
		if(isPlayer && sender.hasPermission("bungeeipc.command.ipcb.status.info")) {
			builder.append("-i|--info|server name");
		}
		else if(isPlayer) {
			builder.append("server name");
		}
		else {
			builder.append("-i|--info");
		}
		sender.sendMessage(builder.color(ChatColor.RED).append("]").color(ChatColor.RED).create());
	}
	
	private void listServers(final CommandSender sender, final boolean isPlayer, final boolean showInfo) {
		
		sender.sendMessage(new ComponentBuilder("================================================").color(ChatColor.DARK_GRAY).create());
		sender.sendMessage(new ComponentBuilder("Minecraft servers attached to the proxy").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("------------------------------------------------").color(ChatColor.DARK_GRAY).create());
		
		if(showInfo) {
			showInfo(sender);
		}
		
		if(isPlayer) {
			final String serverName = ((ProxiedPlayer) sender).getServer().getInfo().getName();
			sender.sendMessage(new ComponentBuilder("Current server: ").color(ChatColor.WHITE).append(serverName).color(getColor(serverName)).create());
			sender.sendMessage(new ComponentBuilder("------------------------------------------------").color(ChatColor.DARK_GRAY).create());
		}
		
		final Map<String, ServerInfo> servers = ipcPlugin.getProxy().getServers();
		boolean canViewOneServer = false;
		
		for(final String serverName : servers.keySet()) {
			if(!servers.get(serverName).canAccess(sender)) {
				continue;
			}
			canViewOneServer = true;
			sender.sendMessage(new ComponentBuilder(" - ").color(ChatColor.WHITE).append(serverName).color(getColor(serverName)).create());
		}
		
		if(!canViewOneServer) {
			sender.sendMessage(new ComponentBuilder("No servers.").color(ChatColor.RED).create());
		}
		sender.sendMessage(new ComponentBuilder("------------------------------------------------").color(ChatColor.DARK_GRAY).create());
	}
	
	private void showInfo(final CommandSender sender) {
		sender.sendMessage(new ComponentBuilder("GRAY").color(ChatColor.GRAY).append(": no information").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("RED").color(ChatColor.RED).append(": offline").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("BLUE").color(ChatColor.BLUE).append(": online, non-IPC").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("GOLD").color(ChatColor.GOLD).append(": online, IPC, not available").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("YELLOW").color(ChatColor.YELLOW).append(": online, IPC, not connected").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("GREEN").color(ChatColor.GREEN).append(": online, IPC, connected").color(ChatColor.WHITE).create());
		sender.sendMessage(new ComponentBuilder("------------------------------------------------").color(ChatColor.DARK_GRAY).create());
	}
	
	private ChatColor getColor(final String serverName) {
		
		final Map<String, Boolean> onlineStatuses = ipcPlugin.getOnlineStatuses();
		final Map<String, IPCServer> ipcServers = ipcPlugin.getServers();
		
		if(!onlineStatuses.containsKey(serverName)) {
			return ChatColor.GRAY;
		}
		else if(!onlineStatuses.get(serverName).booleanValue()) {
			return ChatColor.RED;
		}
		else if(!ipcServers.containsKey(serverName)) {
			return ChatColor.BLUE;
		}
		else if(!ipcServers.get(serverName).isAvailable()) {
			return ChatColor.GOLD;
		}
		else if(!ipcServers.get(serverName).isConnected()) {
			return ChatColor.YELLOW;
		}
		else {
			return ChatColor.GREEN;
		}
	}
	
	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		
		final ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
		final ArrayList<String> completions = new ArrayList<String>();
		
		final boolean permissionCommand = sender.hasPermission("bungeeipc.command.ipcb.command");
		final boolean permissionStatus = sender.hasPermission("bungeeipc.command.ipcb.status");
		final boolean permissionReconnect = sender.hasPermission("bungeeipc.command.ipcb.reconnect");
		
		if(permissionCommand) {
			completions.add("command");
		}
		if(permissionStatus) {
			completions.add("status");
		}
		if(permissionReconnect) {
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
		if(!permissionCommand && !permissionStatus && !permissionReconnect) {
			return completions;
		}
		
		for(final ServerInfo server : ipcPlugin.getProxy().getServers().values()) {
			if(server.canAccess(sender)) {
				completions.add(server.getName());
			}
		}
		
		if(argList.isEmpty()) {
			return completions;
		}
		
		final String serverName = argList.remove(0);
		if(argList.isEmpty()) {
			
			final Iterator<String> iterator = completions.iterator();
			while(iterator.hasNext()) {
				if(!iterator.next().toLowerCase().startsWith(serverName.toLowerCase())) {
					iterator.remove();
				}
			}
			
			return completions;
		}
		
		completions.clear();
		
		if(permissionCommand && subCommand.equalsIgnoreCase("command")) {
			
			for(final ProxiedPlayer player : ipcPlugin.getProxy().getPlayers()) {
				completions.add(player.getName());
			}
			
			if(argList.isEmpty()) {
				return completions;
			}
			
			final String playerName = argList.remove(0);
			if(argList.isEmpty()) {
				
				final Iterator<String> iterator = completions.iterator();
				while(iterator.hasNext()) {
					if(!iterator.next().toLowerCase().startsWith(playerName.toLowerCase())) {
						iterator.remove();
					}
				}
				
				return completions;
			}
			
			completions.clear();
			return completions;
		}
		else if(permissionStatus && subCommand.equalsIgnoreCase("status")) {
			return completions;
		}
		else if(permissionReconnect && subCommand.equalsIgnoreCase("reconnect")) {
			return completions;
		}
		else {
			return completions;
		}
	}
}

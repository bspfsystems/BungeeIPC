/*
 * BungeeIPC BungeeCord/Bukkit plugin for Minecraft
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

package org.unixminecraft.bungeeipc.bungeeproxy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unixminecraft.bungeeipc.bungeeproxy.BungeeIPCPlugin;
import org.unixminecraft.bungeeipc.bungeeproxy.api.IPCServer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class ServerCommand extends Command implements TabExecutor {
	
	private final BungeeIPCPlugin ipcPlugin;
	
	public ServerCommand(final BungeeIPCPlugin ipcPlugin) {
		
		super("server", "bungeeipc.command.server");
		this.ipcPlugin = ipcPlugin;
	}
	
	@Override
	public void execute(final CommandSender sender, final String[] args) {
		
		final boolean isPlayer = sender instanceof ProxiedPlayer;
		if(args.length == 0) {
			listServers(sender, isPlayer, false);
		}
		else if(args.length == 1) {
			if(args[0].equals("-i") || args[0].equals("--info")) {
				listServers(sender, isPlayer, true);
			}
			else if(!isPlayer) {
				sendSyntax(sender, isPlayer);
			}
			else {
				changeServers((ProxiedPlayer) sender, args[0]);
			}
		}
		else {
			sendSyntax(sender, isPlayer);
		}
	}
	
	private void sendSyntax(final CommandSender sender, final boolean isPlayer) {
		
		final ComponentBuilder builder = new ComponentBuilder("Syntax: /server [").color(ChatColor.RED);
		if(isPlayer && sender.hasPermission("bungeeipc.command.server.info")) {
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
	
	private void changeServers(final ProxiedPlayer player, final String serverName) {
		
		final ServerInfo serverInfo = ipcPlugin.getProxy().getServerInfo(serverName);
		if(serverInfo == null) {
			player.sendMessage(new ComponentBuilder("Server ").color(ChatColor.RED).append(serverName).color(ChatColor.GOLD).append(" not found.").color(ChatColor.RED).create());
		}
		else if(!serverInfo.canAccess(player)) {
			player.sendMessage(new ComponentBuilder("You do not have permission to access that server.").color(ChatColor.RED).create());
		}
		else {
			player.connect(serverInfo, ServerConnectEvent.Reason.COMMAND);
		}
	}
	
	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		
		if(sender instanceof ProxiedPlayer) {
			if(args.length == 0) {
				return getServers(sender, "");
			}
			else if(args.length == 1) {
				if(args[0].startsWith("-") && sender.hasPermission("bungeeipc.command.server.info")) {
					return getInfoCompletions(args[0].toLowerCase());
				}
				else {
					return getServers(sender, args[0].toLowerCase());
				}
			}
			else {
				return Collections.emptyList();
			}
		}
		else if(args.length == 0) {
			return Collections.emptyList();
		}
		else {
			return getInfoCompletions(args[0]);
		}

	}
	
	private Set<String> getServers(final CommandSender sender, final String buffer) {
		
		final Map<String, ServerInfo> servers = new HashMap<String, ServerInfo>();
		servers.putAll(ipcPlugin.getProxy().getServers());
		
		final Iterator<String> iterator = servers.keySet().iterator();
		while(iterator.hasNext()) {
			
			final String serverName = iterator.next();
			if(!serverName.toLowerCase().startsWith(buffer) || !servers.get(serverName).canAccess(sender)) {
				iterator.remove();
			}
		}
		
		return servers.keySet();
	}
	
	private List<String> getInfoCompletions(final String buffer) {
		
		final List<String> completions = new ArrayList<String>();
		if("-i".startsWith(buffer)) {
			completions.add("-i");
		}
		if("--info".startsWith(buffer)) {
			completions.add("--info");
		}
		return completions;
	}
}

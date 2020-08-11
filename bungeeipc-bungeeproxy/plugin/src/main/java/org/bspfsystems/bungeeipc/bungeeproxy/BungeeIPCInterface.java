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

package org.bspfsystems.bungeeipc.bungeeproxy;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCInterface;
import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCMessage;

import net.md_5.bungee.api.CommandSender;

final class BungeeIPCInterface implements IPCInterface {
	
	private final BungeeIPCPlugin ipcPlugin;
	private final Logger logger;
	
	BungeeIPCInterface(final BungeeIPCPlugin ipcPlugin) {
		this.ipcPlugin = ipcPlugin;
		this.logger = ipcPlugin.getLogger();
	}
	
	@Override
	public void receiveMessage(final IPCMessage ipcMessage) {
		
		final String server = ipcMessage.getServer();
		final String channel = ipcMessage.getChannel();
		final List<String> messages = ipcMessage.getMessages();
		
		if(messages.isEmpty()) {
			logger.log(Level.INFO, "Empty IPC Message sent to the proxy.");
			logger.log(Level.INFO, server + IPCMessage.SEPARATOR + ipcMessage.toString());
			return;
		}
		
		if(channel.equals("PROXY_COMMAND")) {
			if(messages.size() < 2) {
				logger.log(Level.WARNING, "Incomplete IPC proxy command sent to proxy.");
				logger.log(Level.WARNING, server + IPCMessage.SEPARATOR + ipcMessage.toString());
				return;
			}
			
			final String senderId = messages.remove(0);
			final CommandSender sender;
			if(senderId.equals("console")) {
				sender = ipcPlugin.getProxy().getConsole();
			}
			else {
				final UUID playerId;
				try {
					playerId = UUID.fromString(senderId);
				}
				catch(IllegalArgumentException e) {
					logger.log(Level.WARNING, "Unable to decipher command sender UUID.");
					logger.log(Level.WARNING, "Incoming value: " + senderId);
					logger.log(Level.WARNING, "IllegalArgumentException thrown.", e);
					return;
				}
				sender = ipcPlugin.getProxy().getPlayer(playerId);
			}
			
			if(sender == null) {
				logger.log(Level.WARNING, "Unable to find suitable command sender for proxy command.");
				logger.log(Level.WARNING, "Incoming value: " + senderId);
				return;
			}
			
			String command = messages.remove(0);
			final Iterator<String> iterator = messages.iterator();
			while(iterator.hasNext()) {
				command += " " + iterator.next();
			}
			
			ipcPlugin.getProxy().getPluginManager().dispatchCommand(sender, command);
		}
		else if(channel.equals("FORWARD_IPC_MESSAGE")) {
			
			if(messages.size() < 3) {
				logger.log(Level.WARNING, "Incomplete IPC forward request sent to proxy.");
				logger.log(Level.WARNING, server + IPCMessage.SEPARATOR + ipcMessage.toString());
				return;
			}
			
			final String toServer = messages.remove(0);
			if(!ipcPlugin.isRegisteredServer(toServer)) {
				logger.log(Level.WARNING, "Forward request is to an unregistered IPC server.");
				logger.log(Level.WARNING, "Requested server: " + toServer);
				return;
			}
			
			final String toChannel = messages.remove(0);
			ipcPlugin.sendMessage(new IPCMessage(toServer, toChannel, messages));
		}
		else {
			
			logger.log(Level.WARNING, "IPC Message sent to proxy, but not to a registered channel.");
			logger.log(Level.WARNING, server + IPCMessage.SEPARATOR + ipcMessage.toString());
		}
	}
}

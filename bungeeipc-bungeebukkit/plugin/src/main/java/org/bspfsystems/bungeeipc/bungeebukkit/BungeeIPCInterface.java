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

package org.bspfsystems.bungeeipc.bungeebukkit;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bspfsystems.bungeeipc.bungeebukkit.api.IPCInterface;
import org.bspfsystems.bungeeipc.bungeebukkit.api.IPCMessage;
import org.bukkit.command.CommandSender;

final class BungeeIPCInterface implements IPCInterface {
	
	private final BungeeIPCPlugin ipcPlugin;
	private final Logger logger;
	
	BungeeIPCInterface(final BungeeIPCPlugin ipcPlugin) {
		this.ipcPlugin = ipcPlugin;
		this.logger = ipcPlugin.getLogger();
	}
	
	@Override
	public void receiveMessage(final IPCMessage ipcMessage) {
		
		final String channel = ipcMessage.getChannel();
		final List<String> messages = ipcMessage.getMessages();
		
		if(messages.isEmpty()) {
			logger.log(Level.INFO, "Empty IPC Message sent to the server.");
			logger.log(Level.INFO, ipcMessage.toString());
			return;
		}
		
		if(channel.equals("SERVER_COMMAND")) {
			if(messages.size() < 2) {
				logger.log(Level.WARNING, "Incomplete IPC server sommand sent to the server.");
				logger.log(Level.WARNING, ipcMessage.toString());
				return;
			}
			
			final String senderId = messages.remove(0);
			final CommandSender sender;
			if(senderId.equals("console")) {
				sender = ipcPlugin.getServer().getConsoleSender();
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
				sender = ipcPlugin.getServer().getPlayer(playerId);
			}
			
			if(sender == null) {
				logger.log(Level.WARNING, "Unable to find sutiable command sender for server command.");
				logger.log(Level.WARNING, "Incoming value: " + senderId);
				return;
			}
			
			String command = messages.remove(0);
			final Iterator<String> iterator = messages.iterator();
			while(iterator.hasNext()) {
				command += " " + iterator.next();
			}
			
			ipcPlugin.getServer().dispatchCommand(sender, command);
		}
		else {
			
			logger.log(Level.WARNING, "IPC Message sent to server, but not a registered channel.");
			logger.log(Level.WARNING, ipcMessage.toString());
		}
	}
}

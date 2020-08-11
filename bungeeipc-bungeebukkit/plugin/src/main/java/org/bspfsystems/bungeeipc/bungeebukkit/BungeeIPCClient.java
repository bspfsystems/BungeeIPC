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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bspfsystems.bungeeipc.bungeebukkit.api.IPCClient;
import org.bspfsystems.bungeeipc.bungeebukkit.api.IPCMessage;
import org.bukkit.configuration.file.YamlConfiguration;

final class BungeeIPCClient implements IPCClient {
	
	private final BungeeIPCPlugin ipcPlugin;
	private final Logger logger;
	
	private final InetAddress address;
	private final int port;
	
	private DataOutputStream outToBungee;
	private Socket socket;
	
	private final AtomicBoolean available;
	private final AtomicBoolean connected;
	
	private int taskId;
	
	BungeeIPCClient(final BungeeIPCPlugin ipcPlugin, final YamlConfiguration config) {
		
		this.ipcPlugin = ipcPlugin;
		this.logger = ipcPlugin.getLogger();
		
		if(config == null) { 
			throw new IllegalArgumentException("IPCClient configuration cannot be null.");
		}
		
		if(!config.contains("ip_address")) {
			throw new IllegalArgumentException("IPCClient configuration missing IP address.");
		}
		if(!config.contains("port")) {
			throw new IllegalArgumentException("IPCClient configuration missing port number.");
		}
		
		final Object addressValueRaw = config.get("ip_address");
		final Object portRaw = config.get("port");
		
		if(addressValueRaw == null) {
			throw new IllegalArgumentException("IPCClient IP address is null.");
		}
		if(portRaw == null) {
			throw new IllegalArgumentException("IPCClient port number is null.");
		}
		
		if(!(addressValueRaw instanceof String)) {
			throw new IllegalArgumentException("IPCClient IP address cannot be used.");
		}
		if(!(portRaw instanceof Integer)) {
			throw new IllegalArgumentException("IPCClient port number cannot be used.");
		}
		
		final InetAddress address;
		try {
			address = InetAddress.getByName((String) addressValueRaw);
		}
		catch(UnknownHostException e) {
			throw new IllegalArgumentException("IPCClient IP address threw UnknownHostException.", e);
		}
		catch(SecurityException e) {
			throw new IllegalArgumentException("IPCClient IP address threw SecurityException.", e);
		}
		final int port = ((Integer) portRaw).intValue();
		
		if(port < 1024 || port > 65535) { 
			throw new IllegalArgumentException("IPCClient port number is not between 1024 and 65535 (inclusive).");
		}
		
		this.address = address;
		this.port = port;
		
		this.available = new AtomicBoolean();
		this.connected = new AtomicBoolean();
		this.outToBungee = null;
	}
	
	@Override
	public boolean isAvailable() {
		return available.get();
	}
	
	@Override
	public boolean isConnected() {
		return available.get() && connected.get();
	}
	
	@Override
	public int start() {
		taskId = ipcPlugin.getServer().getScheduler().runTaskAsynchronously(ipcPlugin, this).getTaskId();
		return taskId;
	}
	
	@Override
	public void run() {
		
		available.set(true);
		connected.set(false);
		
		while(available.get()) {
			
			DataInputStream inFromBungee = null;
			
			try {
				socket = new Socket(address, port);
				connected.set(true);
				
				outToBungee = new DataOutputStream(socket.getOutputStream());
				inFromBungee = new DataInputStream(socket.getInputStream());
				
				while(connected.get()) {
					final IPCMessage ipcMessage = IPCMessage.fromString(inFromBungee.readUTF());
					ipcPlugin.getServer().getScheduler().runTask(ipcPlugin, new Runnable() {
						
						@Override
						public void run() {
							ipcPlugin.receiveMessage(ipcMessage);
						}
					});
				}
			}
			catch(IOException e) {
				logger.log(Level.INFO, "IPC connection broken.");
				logger.log(Level.INFO, "IP Address  - " + address.getHostAddress());
				logger.log(Level.INFO, "Port Number - " + String.valueOf(port));
				
				try {
					if(outToBungee != null) {
						outToBungee.close();
					}
				}
				catch(IOException e1) {
					logger.log(Level.WARNING, "Failure while attempting to close the output stream to Bungee after the IPC connection was broken.");
					logger.log(Level.WARNING, "IOException thrown.", e1);
				}
				try {
					if(inFromBungee != null) {
						inFromBungee.close();
					}
				}
				catch(IOException e1) {
					logger.log(Level.WARNING, "Failure while attempting to close the input stream from Bungee after the IPC connection was broken.");
					logger.log(Level.WARNING, "IOException thrown.", e1);
				}
				try {
					if(socket != null) {
						socket.close();
					}
				}
				catch(IOException e1) {
					logger.log(Level.WARNING, "Failure while attempting to close the socket to Bungee after the IPC connection was broken.");
					logger.log(Level.WARNING, "IOException thrown.", e1);
				}
				
				connected.set(false);
				outToBungee = null;
			}
		}
	}
	
	@Override
	public void stop() {
		
		ipcPlugin.getServer().getScheduler().cancelTask(taskId);
		
		available.set(false);
		connected.set(false);
		
		try {
			if(socket != null) {
				socket.close();
			}
		}
		catch(IOException e1) {
			logger.log(Level.WARNING, "Failure while attempting to close the socket to Bungee during shutdown.");
			logger.log(Level.WARNING, "IOException thrown.", e1);
		}
	}
	
	@Override
	public synchronized void sendMessage(final IPCMessage ipcMessage) {
		
		ipcPlugin.getServer().getScheduler().runTaskAsynchronously(ipcPlugin, new Runnable() { 
			
			@Override
			public void run() {
				send(ipcMessage);
			}
		});
	}
	
	private synchronized void send(final IPCMessage ipcMessage) {
		
		if(!connected.get()) {
			logger.log(Level.WARNING, "Unable to send IPCMessage.");
			logger.log(Level.WARNING, "IPCClient not connected.");
			return;
		}
		if(outToBungee == null) {
			logger.log(Level.SEVERE, "Unable to send IPCMessage.");
			logger.log(Level.SEVERE, "IPCClient output to Bungee is null.");
			logger.log(Level.SEVERE, "Client check determined that the connection is valid.");
			return;
		}
		
		try {
			outToBungee.writeUTF(ipcMessage.toString());
		}
		catch(IOException e) {
			logger.log(Level.WARNING, "Cannot send message to Bungee proxy.");
			logger.log(Level.WARNING, "IOException thrown.", e);
		}
	}
}

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCMessage;
import org.bspfsystems.bungeeipc.bungeeproxy.api.IPCServer;

import net.md_5.bungee.config.Configuration;

final class BungeeIPCServer implements IPCServer {
	
	private final BungeeIPCPlugin ipcPlugin;
	private final Logger logger;
	
	private final String serverName;
	private final InetAddress address;
	private final int port;
	
	private DataOutputStream outToBukkit;
	private ServerSocket serverSocket;
	private Socket socket;
	
	private boolean available;
	private boolean connected;
	
	private int taskId;
	
	BungeeIPCServer(final BungeeIPCPlugin ipcPlugin, final Configuration config) {
		
		this.ipcPlugin = ipcPlugin;
		this.logger = ipcPlugin.getLogger();
		
		if(config == null) {
			throw new IllegalArgumentException("IPCServer configuration cannot be null.");
		}
		
		if(!config.contains("server_name")) {
			throw new IllegalArgumentException("IPCServer configuration missing server name.");
		}
		if(!config.contains("ip_address")) {
			throw new IllegalArgumentException("IPCServer configuration missing IP address.");
		}
		if(!config.contains("port")) {
			throw new IllegalArgumentException("IPCServer configuration missing port number.");
		}
		
		final Object serverNameRaw = config.get("server_name");
		final Object addressValueRaw = config.get("ip_address");
		final Object portRaw = config.get("port");
		
		if(serverNameRaw == null) {
			throw new IllegalArgumentException("IPCServer server name is null.");
		}
		if(addressValueRaw == null) {
			throw new IllegalArgumentException("IPCServer IP address is null.");
		}
		if(portRaw == null) {
			throw new IllegalArgumentException("IPCServer port number is null.");
		}
		
		if(!(serverNameRaw instanceof String)) {
			throw new IllegalArgumentException("IPCServer server name cannot be used.");
		}
		if(!(addressValueRaw instanceof String)) {
			throw new IllegalArgumentException("IPCServer IP address cannot be used.");
		}
		if(!(portRaw instanceof Integer)) {
			throw new IllegalArgumentException("IPCServer port number cannot be used.");
		}
		
		final String serverName = ((String) serverNameRaw).toLowerCase();
		final InetAddress address;
		try {
			address = InetAddress.getByName((String) addressValueRaw);
		}
		catch(UnknownHostException e) {
			throw new IllegalArgumentException("IPCServer IP address threw UnknownHostException.", e);
		}
		catch(SecurityException e) {
			throw new IllegalArgumentException("IPCServer IP address threw SecurityException.", e);
		}
		final int port = ((Integer) portRaw).intValue();
		
		if(ipcPlugin.getProxy().getServerInfo(serverName) == null) {
			throw new IllegalArgumentException("IPCServer server name is not a server registered to this BungeeCord proxy.");
		}
		if(!ipcPlugin.getLocalAddresses().contains(address) && !address.isLoopbackAddress()) {
			throw new IllegalArgumentException("IPCServer IP address is not a local or loopback address.");
		}
		if(port < 1024 || port > 65535) {
			throw new IllegalArgumentException("IPCServer port number is not between 1024 and 65535 (inclusive).");
		}
		
		this.serverName = serverName;
		this.address = address;
		this.port = port;
		
		available = false;
		connected = false;
		outToBukkit = null;
	}
	
	@Override
	public String getName() {
		return serverName;
	}
	
	InetAddress getAddress() {
		return address;
	}
	
	int getPort() {
		return port;
	}
	
	@Override
	public boolean isAvailable() {
		return available;
	}
	
	@Override
	public boolean isConnected() {
		return available && connected;
	}
	
	@Override
	public int start() {
		taskId = ipcPlugin.getProxy().getScheduler().runAsync(ipcPlugin, this).getId();
		return taskId;
	}
	
	@Override
	public void run() {
		
		try {
			serverSocket = new ServerSocket(port, 2, address);
		}
		catch(IOException e) {
			logger.log(Level.SEVERE, "IOException thrown while setting up ServerSocket.", e);
			throw new RuntimeException("IOException thrown while setting up ServerSocket.", e);
		}
		
		available = true;
		connected = false;
		
		while(available) {
			try {
				
				socket = serverSocket.accept();
				connected = true;
				
				final DataInputStream inFromBukkit = new DataInputStream(socket.getInputStream());
				outToBukkit = new DataOutputStream(socket.getOutputStream());
				
				while(connected) {
					
					final IPCMessage ipcMessage = IPCMessage.fromString(serverName, inFromBukkit.readUTF());
					ipcPlugin.getProxy().getScheduler().runAsync(ipcPlugin, new Runnable() {
						
						@Override
						public void run() {
							ipcPlugin.receiveMessage(ipcMessage);
						}
					});
				}
			}
			catch(IOException e) {
				
				logger.log(Level.INFO, "IPCConnection broken.");
				logger.log(Level.INFO, "Server Name - " + serverName);
				logger.log(Level.INFO, "IP Address  - " + address.getHostAddress());
				logger.log(Level.INFO, "Port Number - " + String.valueOf(port));
				
				try {
					if(socket != null) {
						socket.close();
					}
				}
				catch(IOException e1) {
					logger.log(Level.WARNING, "Failure while attempting to close Socket after connection was broken.");
					logger.log(Level.WARNING, "IOException thrown.", e1);
				}
				
				connected = false;
				outToBukkit = null;
			}
		}
	}
	
	@Override
	public void stop() {
		
		ipcPlugin.getProxy().getScheduler().cancel(taskId);
		
		available = false;
		connected = false;
		
		try {
			if(socket != null) {
				socket.close();
			}
		}
		catch(IOException e) {
			logger.log(Level.WARNING, "Failure while attempting to close Socket during shutdown.");
			logger.log(Level.WARNING, "IOException thrown.", e);
		}
		
		try {
			if(serverSocket != null) {
				serverSocket.close();
			}
		}
		catch(IOException e) {
			logger.log(Level.WARNING, "Failure while attempting to close ServerSocket during shutdown.");
			logger.log(Level.WARNING, "IOException thrown.", e);
		}
	}
	
	@Override
	public synchronized void sendMessage(final IPCMessage ipcMessage) {
		
		ipcPlugin.getProxy().getScheduler().runAsync(ipcPlugin, new Runnable() {
			
			@Override
			public void run() {
				send(ipcMessage);
			}
		});
	}
	
	private synchronized void send(final IPCMessage ipcMessage) {
		
		if(!connected) {
			logger.log(Level.WARNING, "Unable to send IPCMessage.");
			logger.log(Level.WARNING, "IPCServer is not connected.");
			return;
		}
		if(outToBukkit == null) {
			logger.log(Level.SEVERE, "Unable to send IPCMessage.");
			logger.log(Level.SEVERE, "IPCServer output to Bukkit is null.");
			logger.log(Level.SEVERE, "Server check determined that the connection is valid.");
			return;
		}
		
		try {
			outToBukkit.writeUTF(ipcMessage.toString());
		}
		catch(IOException e) {
			logger.log(Level.WARNING, "Cannot send message to Bukkit server " + serverName);
			logger.log(Level.WARNING, "IOException thrown.");
		}
	}
}

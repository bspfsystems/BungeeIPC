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

package org.unixminecraft.bungeeipc.bungeeproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.unixminecraft.bungeeipc.bungeeproxy.api.IPCInterface;
import org.unixminecraft.bungeeipc.bungeeproxy.api.IPCMessage;
import org.unixminecraft.bungeeipc.bungeeproxy.api.IPCPlugin;
import org.unixminecraft.bungeeipc.bungeeproxy.api.IPCServer;
import org.unixminecraft.bungeeipc.bungeeproxy.command.IPCBCommand;
import org.unixminecraft.bungeeipc.bungeeproxy.command.ServerCommand;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public final class BungeeIPCPlugin extends Plugin implements IPCPlugin {
	
	private Logger logger;
	private ConfigurationProvider provider;
	
	private Collection<InetAddress> localAddresses;
	
	private ConcurrentHashMap<String, BungeeIPCServer> ipcServers;
	private ConcurrentHashMap<String, IPCInterface> ipcInterfaces;
	
	private ConcurrentHashMap<String, Boolean> onlineStatuses;
	
	private ServerStatusUpdater serverStatusUpdater;
	
	public BungeeIPCPlugin() {
		super();
	}
	
	@Override
	public void onEnable() {
		
		logger = getLogger();
		
		logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
		logger.log(Level.INFO, "//                                                                       //");
		logger.log(Level.INFO, "// BungeeIPC BungeeCord/Bukkit plugin for Minecraft                      //");
		logger.log(Level.INFO, "// Copyright (C) 2020  Matt Ciolkosz (https://github.com/mciolkosz)      //");
		logger.log(Level.INFO, "//                                                                       //");
		logger.log(Level.INFO, "// This program is free software: you can redistribute it and/or modify  //");
		logger.log(Level.INFO, "// it under the terms of the GNU General Public License as published by  //");
		logger.log(Level.INFO, "// the Free Software Foundation, either version 3 of the License, or     //");
		logger.log(Level.INFO, "// (at your option) any later version.                                   //");
		logger.log(Level.INFO, "//                                                                       //");
		logger.log(Level.INFO, "// This program is distributed in the hope that it will be useful,       //");
		logger.log(Level.INFO, "// but WITHOUT ANY WARRANTY; without even the implied warranty of        //");
		logger.log(Level.INFO, "// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         //");
		logger.log(Level.INFO, "// GNU General Public License for more details.                          //");
		logger.log(Level.INFO, "//                                                                       //");
		logger.log(Level.INFO, "// You should have received a copy of the GNU General Public License     //");
		logger.log(Level.INFO, "// along with this program.  If not, see <http://www.gnu.org/licenses/>. //");
		logger.log(Level.INFO, "//                                                                       //");
		logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
		
		final ProxyServer proxy = getProxy();
		localAddresses = new ArrayList<InetAddress>();
		
		try {
			for(final NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				localAddresses.addAll(Collections.list(netInterface.getInetAddresses()));
			}
		}
		catch(SocketException e) {
			logger.log(Level.SEVERE, "Unable to load all local network interfaces.");
			proxy.stop();
			return;
		}
		
		final File serverConfigFolder = new File(getDataFolder(), "IPC_Servers");
		final String serverConfigFolderPath = serverConfigFolder.getPath();
		try {
			if(!serverConfigFolder.exists()) {
				logger.log(Level.SEVERE, "IPCServer config folder does not exist at " + serverConfigFolderPath);
				proxy.stop();
				return;
			}
		}
		catch(SecurityException e) {
			logger.log(Level.SEVERE, "Cannot validate existence of IPCServer config folder at " + serverConfigFolderPath);
			proxy.stop();
			return;
		}
		
		final ArrayList<File> serverConfigFiles = new ArrayList<File>(Arrays.asList(serverConfigFolder.listFiles()));
		if(serverConfigFiles.isEmpty()) {
			logger.log(Level.SEVERE, "No IPCServer config files found in IPCServer config folder.");
			proxy.stop();
			return;
		}
		
		provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
		ipcServers = new ConcurrentHashMap<String, BungeeIPCServer>();
		ipcInterfaces = new ConcurrentHashMap<String, IPCInterface>();
		
		boolean allServersLoaded = true;
		final HashSet<String> connections = new HashSet<String>();
		
		for(final File serverConfigFile : serverConfigFiles) {
			
			final String serverConfigFilePath = serverConfigFile.getPath();
			final BungeeIPCServer ipcServer;
			
			try {
				ipcServer = new BungeeIPCServer(this, provider.load(serverConfigFile));
			}
			catch(IOException e) {
				logger.log(Level.SEVERE, "Stopping the proxy.");
				logger.log(Level.SEVERE, "Failure while attempting to load IPCServer config file at " + serverConfigFilePath);
				logger.log(Level.SEVERE, "IOException thrown.", e);
				allServersLoaded = false;
				break;
			}
			catch(IllegalArgumentException e) {
				logger.log(Level.SEVERE, "Stopping the proxy.");
				logger.log(Level.SEVERE, "Failure while attempting to create IPCServer from config file at " + serverConfigFilePath);
				logger.log(Level.SEVERE, "IllegalArgumentException thrown.", e);
				allServersLoaded = false;
				break;
			}
			
			final String serverName = ipcServer.getName();
			final String serverConfigFileName = serverConfigFile.getName().substring(0, serverConfigFile.getName().lastIndexOf(".")).toLowerCase();
			
			if(!ipcServer.getName().equals(serverConfigFileName)) {
				logger.log(Level.SEVERE, "Stopping the proxy.");
				logger.log(Level.SEVERE, "IPCServer config file name and server name mismatch.");
				logger.log(Level.SEVERE, "IPCServer config file name: " + serverConfigFileName);
				logger.log(Level.SEVERE, "IPCServer server name: " + serverName);
				allServersLoaded = false;
				break;
			}
			
			final String connection = ipcServer.getAddress().getHostAddress() + ":" + String.valueOf(ipcServer.getPort());
			if(!connections.add(connection)) {
				logger.log(Level.SEVERE, "Non-unique connection.");
				logger.log(Level.SEVERE, "IPCServer server name: " + serverName);
				allServersLoaded = false;
				break;
			}
			
			if(ipcServers.containsKey(serverName)) {
				logger.log(Level.SEVERE, "Stopping the proxy.");
				logger.log(Level.SEVERE, "IPCServer previously defined and added.");
				logger.log(Level.SEVERE, "Please check all configurations for duplicates.");
				allServersLoaded = false;
				break;
			}
			
			ipcServers.put(serverName, ipcServer);
		}
		
		localAddresses.clear();
		localAddresses = null;
		
		if(!allServersLoaded) {
			proxy.stop();
			return;
		}
		
		for(final IPCServer ipcServer : ipcServers.values()) {
			ipcServer.start();
		}
		
		onlineStatuses = new ConcurrentHashMap<String, Boolean>();
		for(final ServerInfo serverInfo : proxy.getServers().values()) {
			onlineStatuses.put(serverInfo.getName(), Boolean.valueOf(false));
		}
		
		serverStatusUpdater = new ServerStatusUpdater(this);
		
		final PluginManager pluginManager = getProxy().getPluginManager();
		
		final Plugin bungeeServerPlugin = pluginManager.getPlugin("cmd_server");
		if(bungeeServerPlugin != null) {
			pluginManager.unregisterCommands(bungeeServerPlugin);
		}
		
		pluginManager.registerCommand(this, new IPCBCommand(this));
		pluginManager.registerCommand(this, new ServerCommand(this));
		
		final IPCInterface bungeeIPCInterface = new BungeeIPCInterface(this);
		addInterface("PROXY_COMMAND", bungeeIPCInterface);
		addInterface("FORWARD_IPC_MESSAGE", bungeeIPCInterface);
	}
	
	@Override
	public void onDisable() {
		
		removeInterface("PROXY_COMMAND");
		removeInterface("FORWARD_IPC_MESSAGE");
		
		serverStatusUpdater.stop();
		
		for(final IPCServer ipcServer : ipcServers.values()) {
			ipcServer.stop();
		}
	}
	
	Collection<InetAddress> getLocalAddresses() {
		return localAddresses;
	}
	
	@Override
	public boolean isRegisteredServer(final String serverName) {
		return ipcServers.containsKey(serverName.toLowerCase());
	}
	
	@Override
	public void restartServer(final String serverName) {
		
		final IPCServer ipcServer = ipcServers.get(serverName.toLowerCase());
		if(ipcServer == null) {
			return;
		}
		
		ipcServer.stop();
		ipcServer.start();
	}
	
	public Map<String, IPCServer> getServers() {
		return Collections.unmodifiableMap(ipcServers);
	}
	
	public Map<String, Boolean> getOnlineStatuses() {
		return Collections.unmodifiableMap(onlineStatuses);
	}
	
	public void setOnlineStatus(final String serverName, final boolean online) {
		onlineStatuses.put(serverName, Boolean.valueOf(online));
	}
	
	@Override
	public boolean addInterface(final String channel, final IPCInterface ipcInterface) {
		
		if(channel == null) {
			return false;
		}
		if(channel.isEmpty()) {
			return false;
		}
		if(ipcInterface == null) {
			return false;
		}
		if(ipcInterfaces.containsKey(channel)) {
			return false;
		}
		
		return ipcInterfaces.put(channel, ipcInterface) == null;
	}
	
	@Override
	public boolean removeInterface(final String channel) {
		
		if(channel == null) {
			return false;
		}
		if(channel.isEmpty()) {
			return false;
		}
		
		return ipcInterfaces.remove(channel) != null;
	}
	
	@Override
	public void sendMessage(final IPCMessage ipcMessage) {
		
		final String server = ipcMessage.getServer();
		if(server.equals(IPCMessage.BROADCAST_CHANNEL)) {
			broadcastMessage(ipcMessage);
		}
		
		if(!ipcServers.containsKey(server.toLowerCase())) {
			logger.log(Level.WARNING, "Server name " + server + " is not registered as an IPCServer.");
			return;
		}
		
		ipcServers.get(server.toLowerCase()).sendMessage(ipcMessage);
	}
	
	@Override
	public void broadcastMessage(final IPCMessage ipcMessage) {
		for(final IPCServer ipcServer : ipcServers.values()) {
			ipcServer.sendMessage(ipcMessage);
		}
	}
	
	@Override
	public void receiveMessage(final IPCMessage ipcMessage) {
		
		final String channel = ipcMessage.getChannel();
		if(!ipcInterfaces.containsKey(channel)) {
			logger.log(Level.WARNING, "Channel name " + channel + " is not registered as an IPCInterface channel.");
			return;
		}
		
		ipcInterfaces.get(channel).receiveMessage(ipcMessage);
	}
}

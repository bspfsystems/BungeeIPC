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

package org.unixminecraft.bungeeipc.bungeebukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.unixminecraft.bungeeipc.bungeebukkit.api.IPCInterface;
import org.unixminecraft.bungeeipc.bungeebukkit.api.IPCMessage;
import org.unixminecraft.bungeeipc.bungeebukkit.api.IPCPlugin;
import org.unixminecraft.bungeeipc.bungeebukkit.command.IPCCommand;

public final class BungeeIPCPlugin extends JavaPlugin implements IPCPlugin {
	
	private Logger logger;
	
	private BungeeIPCClient ipcClient;
	private ConcurrentHashMap<String, IPCInterface> ipcInterfaces;
	
	private ConcurrentHashMap<String, TabExecutor> commands;
	
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
		
		final Server server = getServer();
		
		final File clientConfigFolder = new File(getDataFolder(), "IPC_Client");
		final String clientConfigFolderPath = clientConfigFolder.getPath();
		try {
			if(!clientConfigFolder.exists()) {
				logger.log(Level.SEVERE, "IPCClient config folder does not exist at " + clientConfigFolderPath);
				server.shutdown();
				return;
			}
		}
		catch(SecurityException e) {
			logger.log(Level.SEVERE, "Cannot validate existence of IPCClient config folder at " + clientConfigFolderPath);
			server.shutdown();
			return;
		}
		
		final File clientConfigFile = new File(clientConfigFolder, "ipc_client.yml");
		final String clientConfigFilePath = clientConfigFile.getPath();
		try {
			if(!clientConfigFile.exists()) {
				logger.log(Level.SEVERE, "IPCClient config file does not exist at " + clientConfigFilePath);
				server.shutdown();
				return;
			}
		}
		catch(SecurityException e) {
			logger.log(Level.SEVERE, "Cannot validate existence of IPCClient config file at " + clientConfigFilePath);
			server.shutdown();
			return;
		}
		
		final YamlConfiguration clientConfig = new YamlConfiguration();
		try {
			clientConfig.load(clientConfigFile);
		}
		catch(FileNotFoundException e) {
			logger.log(Level.SEVERE, "Unable to load IPCClient config file at " + clientConfigFilePath);
			logger.log(Level.SEVERE, "FileNotFoundException thrown.", e);
			server.shutdown();
			return;
		}
		catch(IOException e) {
			logger.log(Level.SEVERE, "Unable to load IPCClient config file at " + clientConfigFilePath);
			logger.log(Level.SEVERE, "IOException thrown.", e);
			server.shutdown();
			return;
		}
		catch(InvalidConfigurationException e) {
			logger.log(Level.SEVERE, "Unable to load IPCClient config file at " + clientConfigFilePath);
			logger.log(Level.SEVERE, "InvalidConfigurationException thrown.", e);
			server.shutdown();
			return;
		}
		catch(IllegalArgumentException e) {
			logger.log(Level.SEVERE, "Unable to load IPCClient config file at " + clientConfigFilePath);
			logger.log(Level.SEVERE, "IllegalArgumentException thrown.", e);
			server.shutdown();
			return;
		}
		
		try {
			ipcClient = new BungeeIPCClient(this, clientConfig);
		}
		catch(IllegalArgumentException e) {
			logger.log(Level.SEVERE, "Unable to create IPCClient.");
			logger.log(Level.SEVERE, "IllegalArgumentException thrown.", e);
			server.shutdown();
			return;
		}
		
		ipcClient.start();
		ipcInterfaces = new ConcurrentHashMap<String, IPCInterface>();
		addInterface("SERVER_COMMAND", new BungeeIPCInterface(this));
		
		commands = new ConcurrentHashMap<String, TabExecutor>();
		
		commands.put("ipc", new IPCCommand(this));
	}
	
	@Override
	public void onDisable() {
		removeInterface("SERVER_COMMAND");
		ipcClient.stop();
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		
		if(!commands.containsKey(command.getName())) {
			return false;
		}
		return commands.get(command.getName()).onCommand(sender, command, label, args);
	}
	
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
		
		if(!commands.containsKey(command.getName())) {
			return null;
		}
		return commands.get(command.getName()).onTabComplete(sender, command, label, args);
	}
	
	@Override
	public void restartClient() {
		ipcClient.stop();
		ipcClient.start();
	}
	
	public boolean isClientAvailable() {
		return ipcClient.isAvailable();
	}
	
	public boolean isClientConnected() {
		return ipcClient.isConnected();
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
		ipcClient.sendMessage(ipcMessage);
	}
	
	@Override
	public void receiveMessage(final IPCMessage ipcMessage) {
		
		final String channel = ipcMessage.getChannel();
		if(!ipcInterfaces.containsKey(channel)) {
			logger.log(Level.WARNING, "Channel name " + channel + " is not registered as an IPCInterface channel.");
			logger.log(Level.WARNING, ipcMessage.toString());
			return;
		}
		
		ipcInterfaces.get(channel).receiveMessage(ipcMessage);
	}
}

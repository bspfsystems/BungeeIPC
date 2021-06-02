/*
 * This file is part of the BungeeIPC plugins for
 * BungeeCord and Bukkit servers for Minecraft.
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

package org.bspfsystems.bungeeipc.bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.bspfsystems.bungeeipc.api.plugin.IPCClientPlugin;
import org.bspfsystems.bungeeipc.bukkit.command.IPCTabExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public final class BukkitIPCPlugin extends JavaPlugin implements IPCClientPlugin {
    
    private Logger logger;
    
    private BukkitScheduler scheduler;
    
    private BukkitIPCSocket socket;
    private ConcurrentHashMap<String, IPCReader> readers;
    
    public BukkitIPCPlugin() {
        super();
    }
    
    @Override
    public void onEnable() {
        
        this.logger = this.getLogger();
    
        this.logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// BungeeIPC BungeeCord/Bukkit plugin for Minecraft                      //");
        this.logger.log(Level.INFO, "// Copyright (C) 2020-2021 BSPF Systems, LLC (https://bspfsystems.org/)  //");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// This program is free software: you can redistribute it and/or modify  //");
        this.logger.log(Level.INFO, "// it under the terms of the GNU General Public License as published by  //");
        this.logger.log(Level.INFO, "// the Free Software Foundation, either version 3 of the License, or     //");
        this.logger.log(Level.INFO, "// (at your option) any later version.                                   //");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// This program is distributed in the hope that it will be useful,       //");
        this.logger.log(Level.INFO, "// but WITHOUT ANY WARRANTY; without even the implied warranty of        //");
        this.logger.log(Level.INFO, "// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         //");
        this.logger.log(Level.INFO, "// GNU General Public License for more details.                          //");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// You should have received a copy of the GNU General Public License     //");
        this.logger.log(Level.INFO, "// along with this program.  If not, see <http://www.gnu.org/licenses/>. //");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
        
        this.scheduler = this.getServer().getScheduler();
        
        this.readers = new ConcurrentHashMap<String, IPCReader>();
        this.addReader("SERVER_COMMAND", new BungeeBukkitIPCReader(this));
        
        final PluginCommand ipcCommand = this.getCommand("ipc");
        if (ipcCommand == null) {
            this.logger.log(Level.SEVERE, "Cannot find /ipc command.");
            throw new RuntimeException("Cannot find the /ipc command.");
        }
        final IPCTabExecutor ipcTabExecutor = new IPCTabExecutor(this);
        ipcCommand.setExecutor(ipcTabExecutor);
        ipcCommand.setTabCompleter(ipcTabExecutor);
        
        this.reloadConfig(this.getServer().getConsoleSender());
    }
    
    @Override
    public void onDisable() {
        this.removeReader("SERVER_COMMAND");
        
        if (this.socket != null) {
            this.socket.stop();
        }
    }
    
    @Override
    public boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader) {
        BukkitIPCPlugin.validateNotBlank(channel, "Channel cannot be blank!");
        if (this.readers.containsKey(channel)) {
            return false;
        }
        return this.readers.put(channel, reader) == null;
    }
    
    @Override
    public boolean removeReader(@NotNull final String channel) {
        BukkitIPCPlugin.validateNotBlank(channel, "Channel cannot be blank!");
        return this.readers.remove(channel) != null;
    }
    
    @Override
    public void sendMessage(@NotNull final IPCMessage message) {
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Unable to send IPC message.");
            this.logger.log(Level.WARNING, "IPC Client not configured.");
            return;
        }
        this.socket.sendMessage(message);
    }
    
    @Override
    public void receiveMessage(@NotNull final IPCMessage message) {
        
        final String channel = message.getChannel();
        final IPCReader reader = this.readers.get(channel);
        if (reader == null) {
            this.logger.log(Level.WARNING, "Channel name " + channel + " is not registered as an IPCReader channel.");
            this.logger.log(Level.WARNING, message.toString());
            return;
        }
        
        reader.readMessage(message);
    }
    
    @Override
    public boolean isClientRunning() {
        return this.socket != null && this.socket.isRunning();
    }
    
    @Override
    public boolean isClientConnected() {
        return this.socket != null && this.socket.isConnected();
    }
    
    @Override
    public void restartClient() {
        
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Cannot restart client, IPCSocket is null.");
            return;
        }
        this.socket.stop();
        
        
        this.scheduler.runTaskLaterAsynchronously(this, this::startClient, 40);
    }
    
    private void startClient() {
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Cannot start client, IPCSocket is null.");
            return;
        }
        this.socket.start();
    }
    
    private static void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public void reloadConfig(@NotNull final CommandSender sender) {
        
        if (this.socket != null) {
            this.socket.stop();
        }
        this.socket = null;
        
        this.scheduler.runTaskAsynchronously(this, () -> {
    
            final File clientConfigDirectory = new File(this.getDataFolder(), "ipcclient");
            try {
                if (!clientConfigDirectory.exists()) {
                    if (!clientConfigDirectory.mkdirs()) {
                        sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                        this.logger.log(Level.WARNING, "IPC Client configuration directory not created at " + clientConfigDirectory.getPath());
                        this.logger.log(Level.WARNING, "IPC Client will not be started.");
                        return;
                    }
                } else if (!clientConfigDirectory.isDirectory()) {
                    sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                    this.logger.log(Level.WARNING, "IPC Client configuration directory is not a directory: " + clientConfigDirectory.getPath());
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    return;
                }
            } catch (SecurityException e) {
                sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                this.logger.log(Level.WARNING, "Cannot validate existence of IPC Client configuration directory at " + clientConfigDirectory.getPath());
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
    
            final File clientConfigFile = new File(clientConfigDirectory, "ipc-client.yml");
            try {
                if (clientConfigFile.exists()) {
                    if (!clientConfigFile.isFile()) {
                        sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                        this.logger.log(Level.WARNING, "IPC Client configuration file is not a file: " + clientConfigFile.getPath());
                        this.logger.log(Level.WARNING, "IPC Client will not be started.");
                        return;
                    }
                } else {
                    if (!clientConfigFile.createNewFile()) {
                        sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                        this.logger.log(Level.WARNING, "IPC Client configuration file not created at " + clientConfigFile.getPath());
                        this.logger.log(Level.WARNING, "IPC Client will not be started.");
                        return;
                    }
            
                    final InputStream defaultConfig = this.getResource(clientConfigFile.getName());
                    final FileOutputStream outputStream = new FileOutputStream(clientConfigFile);
                    final byte[] buffer = new byte[4096];
                    int bytesRead;
            
                    while ((bytesRead = defaultConfig.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
            
                    outputStream.flush();
                    outputStream.close();
            
                    sender.sendMessage("§r§cThe IPC Client configuration file did not exist; a copy of the default has been made and placed in the correct location.§r");
                    sender.sendMessage("§r§cPlease update the configuration as required for the installation, and then run§r §b/ipc reload§r§c.§r");
                    this.logger.log(Level.WARNING, "IPC Client configuration file did not exist at " + clientConfigFile.getPath());
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    this.logger.log(Level.WARNING, "Please update the configuration as required for your installation, and then run \"/ipc reload\".");
                    return;
                }
            } catch (SecurityException | IOException e) {
                sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                this.logger.log(Level.WARNING, "Unable to load the IPC Client configuration file at " + clientConfigFile.getPath());
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
    
            final YamlConfiguration clientConfig = new YamlConfiguration();
            try {
                clientConfig.load(clientConfigFile);
            } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
                sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                this.logger.log(Level.WARNING, "Unable to load IPC Client configuration.");
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            this.scheduler.runTask(this, () -> {
    
                try {
                    this.socket = new BukkitIPCSocket(this, clientConfig);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                    this.logger.log(Level.WARNING, "Unable to create IPC Client.");
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    this.socket = null;
                    return;
                }
    
                this.socket.start();
            });
        });
    }
}

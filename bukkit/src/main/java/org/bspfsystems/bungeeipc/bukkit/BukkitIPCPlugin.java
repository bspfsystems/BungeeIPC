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
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.bspfsystems.bungeeipc.api.plugin.IPCClientPlugin;
import org.bspfsystems.bungeeipc.bukkit.command.IPCTabExecutor;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitIPCPlugin extends JavaPlugin implements IPCClientPlugin {
    
    private Logger logger;
    
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
        
        final Server server = this.getServer();
        
        final File configDirectory = new File(this.getDataFolder(), "IPC_Client");
        try {
            if (!configDirectory.exists()) {
                this.logger.log(Level.SEVERE, "IPCSocket config directory does not exist at " + configDirectory.getPath());
                server.shutdown();
                return;
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Cannot validate existence of IPCSocket config directory at " + configDirectory.getPath(), e);
            server.shutdown();
            return;
        }
        
        final File configFile = new File(configDirectory, "ipc_client.yml");
        try {
            if (!configFile.exists()) {
                this.logger.log(Level.SEVERE, "IPCSocket config file does not exist at " + configFile.getPath());
                server.shutdown();
                return;
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Cannot validate existence of IPCSocket config file at " + configFile.getPath(), e);
            server.shutdown();
            return;
        }
        
        final YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            this.logger.log(Level.SEVERE, "Unable to load IPCSocket config.", e);
            server.shutdown();
            return;
        }
        
        try {
            this.socket = new BukkitIPCSocket(this, config);
        } catch (IllegalArgumentException e) {
            this.logger.log(Level.SEVERE, "Unable to create IPCSocket.", e);
            server.shutdown();
            return;
        }
        
        this.socket.start();
        
        this.readers = new ConcurrentHashMap<String, IPCReader>();
        this.addReader("SERVER_COMMAND", new BungeeBukkitIPCReader(this));
        
        final PluginCommand ipcCommand = this.getCommand("ipc");
        if (ipcCommand == null) {
            this.logger.log(Level.SEVERE, "Cannot find /ipc command.");
            server.shutdown();
            return;
        }
        final IPCTabExecutor ipcTabExecutor = new IPCTabExecutor(this);
        ipcCommand.setExecutor(ipcTabExecutor);
        ipcCommand.setTabCompleter(ipcTabExecutor);
    }
    
    @Override
    public void onDisable() {
        this.removeReader("SERVER_COMMAND");
        this.socket.stop();
    }
    
    @Override
    public boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader) {
        validateNotBlank(channel, "Channel cannot be blank!");
        if (this.readers.containsKey(channel)) {
            return false;
        }
        return this.readers.put(channel, reader) == null;
    }
    
    @Override
    public boolean removeReader(@NotNull final String channel) {
        validateNotBlank(channel, "Channel cannot be blank!");
        return this.readers.remove(channel) != null;
    }
    
    @Override
    public void sendMessage(@NotNull final IPCMessage message) {
        this.socket.sendMessage(message);
    }
    
    @Override
    public void receiveMessage(@NotNull final IPCMessage message) {
        
        final String channel = message.getChannel();
        if (!this.readers.containsKey(channel)) {
            this.logger.log(Level.WARNING, "Channel name " + channel + " is not registered as an IPCReader channel.");
            this.logger.log(Level.WARNING, message.toString());
            return;
        }
        
        this.readers.get(channel).readMessage(message);
    }
    
    @Override
    public boolean isClientRunning() {
        return this.socket.isRunning();
    }
    
    @Override
    public boolean isClientConnected() {
        return this.socket.isConnected();
    }
    
    @Override
    public void restartClient() {
        this.socket.stop();
        this.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> this.socket.start(), 40);
    }
    
    private static void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}

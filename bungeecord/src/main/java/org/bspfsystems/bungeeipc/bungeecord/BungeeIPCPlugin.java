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

package org.bspfsystems.bungeeipc.bungeecord;

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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.bspfsystems.bungeeipc.api.plugin.IPCServerPlugin;
import org.bspfsystems.bungeeipc.api.socket.IPCServerSocket;
import org.bspfsystems.bungeeipc.bungeecord.command.IPCBCommand;
import org.bspfsystems.bungeeipc.bungeecord.command.ServerCommand;
import org.jetbrains.annotations.NotNull;

public final class BungeeIPCPlugin extends Plugin implements IPCServerPlugin {
    
    private static final String PROXY_SERVER = "proxy";
    
    private Logger logger;
    
    private ConcurrentHashMap<String, IPCServerSocket> serverSockets;
    private ConcurrentHashMap<String, IPCReader> readers;
    
    private volatile ConcurrentHashMap<String, AtomicBoolean> onlineStatuses;
    
    private ServerStatusUpdater serverStatusUpdater;
    
    public BungeeIPCPlugin() {
        super();
    }
    
    @Override
    public void onEnable() {
        
        this.logger = this.getLogger();
    
        this.logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// BungeeIPC BungeeCord/Bukkit plugin for Minecraft                      //");
        this.logger.log(Level.INFO, "// Copyright (C) 2020  Matt Ciolkosz (https://github.com/mciolkosz)      //");
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
        
        final ProxyServer proxy = this.getProxy();
        
        final File configDirectory = new File(this.getDataFolder(), "IPC_Servers");
        try {
            if (!configDirectory.exists()) {
                this.logger.log(Level.SEVERE, "IPC Servers config directory does not exist at " + configDirectory.getPath());
                proxy.stop();
                return;
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Unable to validate existence of IPC Servers config directory at " + configDirectory.getPath(), e);
            proxy.stop();
            return;
        }
        
        final Iterator<File> iterator = (new ArrayList<File>(Arrays.asList(configDirectory.listFiles()))).iterator();
        if (!iterator.hasNext()) {
            this.logger.log(Level.SEVERE, "No IPCServerSocket config files found in the IPC Servers config directory.");
            proxy.stop();
            return;
        }
        
        final ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        this.serverSockets = new ConcurrentHashMap<String, IPCServerSocket>();
        final HashSet<String> connections = new HashSet<String>();
        final Collection<InetAddress> localAddresses = new ArrayList<InetAddress>();
    
        try {
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                localAddresses.addAll(Collections.list(iface.getInetAddresses()));
            }
        } catch (SocketException e) {
            this.logger.log(Level.SEVERE, "Unable to load all local network interfaces.");
            proxy.stop();
            return;
        }
        
        while (iterator.hasNext()) {
            
            final File configFile = iterator.next();
            final BungeeIPCServerSocket serverSocket;
            try {
                serverSocket = new BungeeIPCServerSocket(this, provider.load(configFile), localAddresses);
            } catch (IOException e) {
                this.logger.log(Level.SEVERE, "Stopping the proxy.");
                this.logger.log(Level.SEVERE, "Failure while attempting to load the IPCServerSocket config file at " + configFile.getPath());
                this.logger.log(Level.SEVERE, "IOException thrown.", e);
                proxy.stop();
                return;
            } catch (IllegalArgumentException e) {
                this.logger.log(Level.SEVERE, "Stopping the proxy.");
                this.logger.log(Level.SEVERE, "Failure while attempting to load the IPCServerSocket config file at " + configFile.getPath());
                this.logger.log(Level.SEVERE, "IllegalArgumentException thrown.", e);
                proxy.stop();
                return;
            }
            
            final String configFileName = configFile.getName().substring(0, configFile.getName().lastIndexOf(".")).toLowerCase();
            final String serverName = serverSocket.getName();
            if (!configFileName.equals(serverName)) {
                this.logger.log(Level.SEVERE, "Stopping the proxy.");
                this.logger.log(Level.SEVERE, "IPCServerSocket config file name and server name mismatch.");
                this.logger.log(Level.SEVERE, "IPCServerSocket config file name : " + configFileName);
                this.logger.log(Level.SEVERE, "IPCServerSocket name: " + serverName);
                proxy.stop();
                return;
            }
            
            final String connection = serverSocket.getAddress().getHostAddress() + ":" + serverSocket.getPort();
            if (!connections.add(connection)) {
                this.logger.log(Level.SEVERE, "Stopping the proxy.");
                this.logger.log(Level.SEVERE, "Non-unique connection.");
                this.logger.log(Level.SEVERE, "IPCServerSocket name: " + serverName);
                proxy.stop();
                return;
            }
            
            if (this.serverSockets.containsKey(serverName)) {
                this.logger.log(Level.SEVERE, "Stopping the proxy.");
                this.logger.log(Level.SEVERE, "IPCServerSocket previously defined and added.");
                this.logger.log(Level.SEVERE, "Please check all configurations for duplicates.");
                proxy.stop();
                return;
            }
            
            this.serverSockets.put(serverName, serverSocket);
        }
        
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.start();
        }
        
        this.onlineStatuses = new ConcurrentHashMap<String, AtomicBoolean>();
        for (final ServerInfo serverInfo : proxy.getServers().values()) {
            this.onlineStatuses.put(serverInfo.getName(), new AtomicBoolean(false));
        }
        
        this.serverStatusUpdater = new ServerStatusUpdater(this);
        
        final PluginManager pluginManager = this.getProxy().getPluginManager();
        final Plugin bungeeServerPlugin = pluginManager.getPlugin("cmd_server");
        if (bungeeServerPlugin != null) {
            pluginManager.unregisterCommands(bungeeServerPlugin);
        }
        
        pluginManager.registerCommand(this, new IPCBCommand(this));
        pluginManager.registerCommand(this, new ServerCommand(this));
        
        this.readers = new ConcurrentHashMap<String, IPCReader>();
        this.addReader("PROXY_COMMAND", new BungeeProxyIPCReader(this));
    }
    
    @Override
    public void onDisable() {
        this.removeReader("PROXY_COMMAND");
        this.serverStatusUpdater.stop();
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.stop();
        }
    }
    
    @Override
    public boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader) {
        this.validateNotBlank(channel, "Channel cannot be blank!");
        if (this.readers.containsKey(channel)) {
            return false;
        }
        return this.readers.put(channel, reader) == null;
    }
    
    @Override
    public boolean removeReader(@NotNull final String channel) {
        this.validateNotBlank(channel, "Channel cannot be blank!");
        return this.readers.remove(channel) != null;
    }
    
    @Override
    public void sendMessage(@NotNull final IPCMessage message) {
        if (message.getChannel().equals(IPCMessage.BROADCAST_SERVER)) {
            this.broadcastMessage(message);
        } else if (message.getServer().equals(PROXY_SERVER)) {
            this.receiveMessage(message);
        } else if (!this.serverSockets.containsKey(message.getServer())) {
            this.logger.log(Level.WARNING, "Server name " + message.getServer() + " is not registered to this IPC Plugin.");
        } else {
            this.serverSockets.get(message.getServer()).sendMessage(message);
        }
    }
    
    @Override
    public void receiveMessage(@NotNull final IPCMessage message) {
        if (message.getServer().equals(PROXY_SERVER)) {
            if (!this.readers.containsKey(message.getChannel())) {
                this.logger.log(Level.WARNING, "IPC message destined for the BungeeCord proxy, but the channel is not specified.");
                this.logger.log(Level.WARNING, "IPC message channel: " + message.getChannel());
            } else {
                this.readers.get(message.getChannel()).readMessage(message);
            }
        } else if (this.serverSockets.containsKey(message.getServer())) {
            this.logger.log(Level.INFO, "Forwarding message on to server " + message.getServer());
            this.serverSockets.get(message.getServer()).sendMessage(message);
        } else {
            this.logger.log(Level.WARNING, "IPC message with an unregistered server.");
            this.logger.log(Level.WARNING, "IPC message server: " + message.getServer());
            this.logger.log(Level.WARNING, "IPC message data: " + message.toString());
        }
    }
    
    @Override
    public synchronized boolean isRegisteredServer(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank!");
        return this.serverSockets.containsKey(name);
    }
    
    @Override
    public synchronized boolean isServerRunning(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank!");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy!");
        return this.serverSockets.get(name).isRunning();
    }
    
    @Override
    public synchronized boolean isServerConnected(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank!");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy!");
        return this.serverSockets.get(name).isConnected();
    }
    
    @Override
    public void restartServer(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank!");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy!");
        this.serverSockets.get(name).stop();
        this.getProxy().getScheduler().schedule(this, () -> this.serverSockets.get(name).start(), 2L, TimeUnit.SECONDS);
    }
    
    @Override
    public void broadcastMessage(@NotNull final IPCMessage message) {
        if (!message.getChannel().equals(IPCMessage.BROADCAST_SERVER)) {
            this.logger.log(Level.WARNING, "Cannot broadcast IPC message when the server is not the broadcast server.");
            this.logger.log(Level.WARNING, "IPC message server: " + message.getServer());
        } else {
            for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
                serverSocket.sendMessage(message);
            }
        }
    }
    
    private void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    private void validateServer(@NotNull final String name, @NotNull final String message) {
        if (!this.serverSockets.containsKey(name)) {
            throw new IllegalArgumentException(message);
        }
    }
    
    synchronized void setOnlineStatus(@NotNull final String name, final boolean online) {
        this.onlineStatuses.get(name).set(online);
    }
    
    public synchronized int getOnlineStatus(@NotNull final String name) {
        if (!this.onlineStatuses.containsKey(name)) {
            return -1;
        } else if (!this.onlineStatuses.get(name).get()) {
            return 0;
        } else {
            return 1;
        }
    }
}

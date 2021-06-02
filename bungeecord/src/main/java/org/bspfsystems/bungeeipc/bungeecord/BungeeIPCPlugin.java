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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
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
        
        final File mainConfigFile = new File(this.getDataFolder(), "config.yml");
        try {
            if (mainConfigFile.exists()) {
                if (!mainConfigFile.isFile()) {
                    this.logger.log(Level.SEVERE, "Main BungeeIPC configuration file is not a file: " + mainConfigFile.getPath());
                    throw new RuntimeException("Main BungeeIPC configuration file is not a file: " + mainConfigFile.getPath());
                }
            } else {
                if (!mainConfigFile.createNewFile()) {
                    this.logger.log(Level.SEVERE, "Main BungeeIPC configuration file not created at " + mainConfigFile.getPath());
                    throw new RuntimeException("Main BungeeIPC configuration file not created at " + mainConfigFile.getPath());
                }
                
                final InputStream defaultConfig = this.getResourceAsStream(mainConfigFile.getName());
                final FileOutputStream outputStream = new FileOutputStream(mainConfigFile);
                final byte[] buffer = new byte[4096];
                int bytesRead;
        
                while ((bytesRead = defaultConfig.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
        
                outputStream.flush();
                outputStream.close();
            }
        } catch (SecurityException | IOException e) {
            this.logger.log(Level.SEVERE, "Unable to load the main BungeeIPC configuration file at " + mainConfigFile.getPath());
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to load the main BungeeIPC configuration file at " + mainConfigFile.getPath(), e);
        }
    
        final ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        final Configuration mainConfig;
        try {
            mainConfig = provider.load(mainConfigFile);
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "Unable to load the main BungeeIPC configuration.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to load the main BungeeIPC configuration.", e);
        }
        if (mainConfig == null) {
            this.logger.log(Level.SEVERE, "Main BungeeIPC configuration not loaded, no Exception thrown.");
            throw new RuntimeException("Main BungeeIPC configuration not loaded, no Exception thrown.");
        }
        
        SSLServerSocketFactory sslServerSocketFactory = null;
        ArrayList<String> tlsVersionWhitelist = null;
        ArrayList<String> tlsCipherSuiteWhitelist = null;
        
        final boolean useSSL = mainConfig.getBoolean("use_ssl", false);
        if (useSSL) {
            
            final String keyStoreFile = mainConfig.getString("key_store_file", null);
            final String keyStorePassword = mainConfig.getString("key_store_password", null);
            
            if (keyStoreFile == null || keyStoreFile.trim().isEmpty()) {
                this.logger.log(Level.SEVERE, "KeyStore file is null or empty: " + (keyStoreFile == null ? "null" : keyStoreFile));
                throw new RuntimeException("KeyStore file is null or empty: " + (keyStoreFile == null ? "null" : keyStoreFile));
            }
    
            if (keyStorePassword == null || keyStorePassword.trim().isEmpty()) {
                this.logger.log(Level.SEVERE, "KeyStore password is null or empty: " + (keyStorePassword == null ? "null" : keyStorePassword));
                throw new RuntimeException("KeyStore password is null or empty: " + (keyStorePassword == null ? "null" : keyStorePassword));
            }
            
            String keyStoreInstance = mainConfig.getString("key_store_instance", "JKS");
            if (keyStoreInstance == null || keyStoreInstance.trim().isEmpty()) {
                keyStoreInstance = "JKS";
            }
            String keyManagerFactoryAlgorithm = mainConfig.getString("key_manager_factory_algorithm", "NewSunX509");
            if (keyManagerFactoryAlgorithm == null || keyManagerFactoryAlgorithm.trim().isEmpty()) {
                keyManagerFactoryAlgorithm = "NewSunX509";
            }
            String trustManagerFactoryAlgorithm = mainConfig.getString("trust_manager_factory_algorithm", "SunX509");
            if (trustManagerFactoryAlgorithm == null || trustManagerFactoryAlgorithm.trim().isEmpty()) {
                trustManagerFactoryAlgorithm = "SunX509";
            }
            String sslContextProtocol = mainConfig.getString("ssl_context_protocol", "TLS");
            if (sslContextProtocol == null || sslContextProtocol.trim().isEmpty()) {
                sslContextProtocol = "TLS";
            }
    
            final List<String> tlsVersionWhitelistRaw = mainConfig.getStringList("tls_version_whitelist");
            tlsVersionWhitelist = new ArrayList<String>();
    
            if (tlsVersionWhitelistRaw.isEmpty()) {
                tlsVersionWhitelist.add("TLSv1.2");
            } else {
                for (final String version : tlsVersionWhitelistRaw) {
                    if (version == null || version.trim().isEmpty()) {
                        continue;
                    }
                    tlsVersionWhitelist.add(version);
                }
            }
    
            if (tlsVersionWhitelist.isEmpty()) {
                tlsVersionWhitelist.add("TLSv1.2");
            }
    
            final List<String> tlsCipherSuiteWhitelistRaw = mainConfig.getStringList("tls_cipher_suite_whitelist");
            tlsCipherSuiteWhitelist = new ArrayList<String>();
    
            if (tlsCipherSuiteWhitelistRaw.isEmpty()) {
                tlsCipherSuiteWhitelist.add("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");
            } else {
                for (final String cipherSuite : tlsCipherSuiteWhitelistRaw) {
                    if (cipherSuite == null || cipherSuite.trim().isEmpty()) {
                        continue;
                    }
                    tlsCipherSuiteWhitelist.add(cipherSuite);
                }
            }
    
            if (tlsCipherSuiteWhitelist.isEmpty()) {
                tlsCipherSuiteWhitelist.add("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");
            }
            
            try {
                final KeyStore keyStore = KeyStore.getInstance(keyStoreInstance);
                keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
                
                final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
                
                final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
                trustManagerFactory.init(keyStore);
                
                final SSLContext sslContext = SSLContext.getInstance(sslContextProtocol);
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
                
                sslServerSocketFactory = sslContext.getServerSocketFactory();
            } catch (KeyStoreException | SecurityException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
                this.logger.log(Level.SEVERE, "Unable to create SSLServerSocketFactory.");
                this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
                throw new RuntimeException("Unable to create SSLServerSocketFactory.", e);
            }
        }
        
        final File serverConfigDirectory = new File(this.getDataFolder(), "ipcservers");
        try {
            if (!serverConfigDirectory.exists()) {
                this.logger.log(Level.SEVERE, "IPC Servers configuration directory does not exist at " + serverConfigDirectory.getPath());
                throw new RuntimeException("IPC Servers configuration directory does not exist at " + serverConfigDirectory.getPath());
            } else if (!serverConfigDirectory.isDirectory()) {
                this.logger.log(Level.SEVERE, "IPC Servers configuration directory is not a directory: " + serverConfigDirectory.getPath());
                throw new RuntimeException("IPC Servers configuration directory is not a directory: " + serverConfigDirectory.getPath());
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Unable to validate existence of IPC Servers configuration directory at " + serverConfigDirectory.getPath());
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to validate existence of IPC Servers configuration directory at " + serverConfigDirectory.getPath(), e);
        }
        
        final Iterator<File> iterator = (Arrays.asList(serverConfigDirectory.listFiles())).iterator();
        if (!iterator.hasNext()) {
            this.logger.log(Level.SEVERE, "No IPCServerSocket configuration files found in the IPC Servers configuration directory.");
            throw new RuntimeException("No IPCServerSocket configuration files found in the IPC Servers configuration directory.");
        }
        
        this.serverSockets = new ConcurrentHashMap<String, IPCServerSocket>();
        final HashSet<String> connections = new HashSet<String>();
        final Collection<InetAddress> localAddresses = new ArrayList<InetAddress>();
    
        try {
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                localAddresses.addAll(Collections.list(iface.getInetAddresses()));
            }
        } catch (SocketException e) {
            this.logger.log(Level.SEVERE, "Unable to load all local network interfaces.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to load all local network interfaces.", e);
        }
        
        while (iterator.hasNext()) {
            
            final File serverConfigFile = iterator.next();
            final BungeeIPCServerSocket serverSocket;
            try {
                serverSocket = new BungeeIPCServerSocket(this, provider.load(serverConfigFile), localAddresses, sslServerSocketFactory, tlsVersionWhitelist, tlsCipherSuiteWhitelist);
            } catch (IOException | IllegalArgumentException e) {
                this.logger.log(Level.SEVERE, "Failure while attempting to load the IPCServerSocket configuration file at " + serverConfigFile.getPath());
                this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
                throw new RuntimeException("Failure while attempting to load the IPCServerSocket configuration file at " + serverConfigFile.getPath(), e);
            }
            
            final String configFileName = serverConfigFile.getName().substring(0, serverConfigFile.getName().lastIndexOf(".")).toLowerCase();
            final String serverName = serverSocket.getName();
            if (!configFileName.equals(serverName)) {
                this.logger.log(Level.SEVERE, "IPCServerSocket configuration file name and server name mismatch.");
                this.logger.log(Level.SEVERE, "Configuration file name : " + configFileName);
                this.logger.log(Level.SEVERE, "Name within the configuration: " + serverName);
                this.logger.log(Level.SEVERE, "Configuration file path: " + serverConfigFile.getPath());
                throw new RuntimeException("IPCServerSocket and configuration file name mismatch for IPCServerSocket configuration file at " + serverConfigFile.getPath());
            }
            
            final String connection = serverSocket.getAddress().getHostAddress() + ":" + serverSocket.getPort();
            if (!connections.add(connection)) {
                this.logger.log(Level.SEVERE, "Non-unique IPC connection.");
                this.logger.log(Level.SEVERE, "IPCServerSocket name: " + serverName);
                this.logger.log(Level.SEVERE, "Hostname/IP Address: " + serverSocket.getAddress().getHostAddress());
                this.logger.log(Level.SEVERE, "Port: " + serverSocket.getPort());
                this.logger.log(Level.SEVERE, "Configuration file path: " + serverConfigFile.getPath());
                throw new RuntimeException("IPCServerSocket configuration contains non-unique connection information (hostname/IP and port) for IPCServerSocket configuration file at " + serverConfigFile.getPath());
            }
            
            if (this.serverSockets.containsKey(serverName)) {
                this.logger.log(Level.SEVERE, "IPCServerSocket previously defined and added.");
                this.logger.log(Level.SEVERE, "Please check all configurations for duplicates.");
                this.logger.log(Level.SEVERE, "Duplicate IPCServerSocket name: " + serverName);
                this.logger.log(Level.SEVERE, "Configuration file path: " + serverConfigFile.getPath());
                throw new RuntimeException("IPCServerSocket configuration contains non-unique server name for IPCServerSocket configuration file at " + serverConfigFile.getPath());
            }
            
            this.serverSockets.put(serverName, serverSocket);
        }
        
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.start();
        }
        
        this.onlineStatuses = new ConcurrentHashMap<String, AtomicBoolean>();
        for (final ServerInfo serverInfo : this.getProxy().getServers().values()) {
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
        } else if (message.getServer().equals(BungeeIPCPlugin.PROXY_SERVER)) {
            this.receiveMessage(message);
        } else if (!this.serverSockets.containsKey(message.getServer())) {
            this.logger.log(Level.WARNING, "Server name " + message.getServer() + " is not registered to this IPC Plugin.");
        } else {
            this.serverSockets.get(message.getServer()).sendMessage(message);
        }
    }
    
    @Override
    public void receiveMessage(@NotNull final IPCMessage message) {
        
        if (message.getServer().equals(BungeeIPCPlugin.PROXY_SERVER)) {
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

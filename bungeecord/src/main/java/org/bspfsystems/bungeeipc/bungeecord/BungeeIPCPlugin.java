/*
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
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
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCReader;
import org.bspfsystems.bungeeipc.api.server.IPCServerPlugin;
import org.bspfsystems.bungeeipc.api.server.IPCServerSocket;
import org.bspfsystems.bungeeipc.bungeecord.command.IPCBCommand;
import org.bspfsystems.bungeeipc.bungeecord.command.ServerCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the implementation of a BungeeCord {@link Plugin} that provides
 * the {@link IPCServerPlugin} functionality.
 */
public final class BungeeIPCPlugin extends Plugin implements IPCServerPlugin {
    
    private static final String PROXY_SERVER = "proxy";
    
    private Logger logger;
    private AtomicBoolean extraLogging;
    private TaskScheduler scheduler;
    
    private ConcurrentHashMap<String, IPCReader> readers;
    
    private ConcurrentHashMap<String, AtomicBoolean> onlineStatuses;
    private ServerStatusUpdater serverStatusUpdater;
    
    private ConcurrentHashMap<String, IPCServerSocket> serverSockets;
    
    /**
     * Explicitly define the constructor.
     */
    public BungeeIPCPlugin() {
        super();
    }
    
    /**
     * Enables this {@link BungeeIPCPlugin}.
     */
    @Override
    public void onEnable() {
        
        this.logger = this.getLogger();
    
        this.logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// BungeeIPC Bukkit/BungeeCord plugin for Minecraft                      //");
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
        
        this.extraLogging = new AtomicBoolean(false);
        this.scheduler = this.getProxy().getScheduler();
        
        // Command setup
        
        final PluginManager pluginManager = this.getProxy().getPluginManager();
        final Plugin bungeeServerPlugin = pluginManager.getPlugin("cmd_server");
        if (bungeeServerPlugin != null) {
            pluginManager.unregisterCommands(bungeeServerPlugin);
        }
    
        pluginManager.registerCommand(this, new IPCBCommand(this));
        pluginManager.registerCommand(this, new ServerCommand(this));
        
        // IPReader setup
        
        this.readers = new ConcurrentHashMap<String, IPCReader>();
        this.addReader("PROXY_COMMAND", new BungeeProxyIPCReader(this));
        
        // Server status setup
        
        this.onlineStatuses = new ConcurrentHashMap<String, AtomicBoolean>();
        for (final ServerInfo serverInfo : this.getProxy().getServers().values()) {
            this.onlineStatuses.put(serverInfo.getName(), new AtomicBoolean(false));
        }
        
        this.serverStatusUpdater = new ServerStatusUpdater(this);
        
        // Server socket setup
        
        this.serverSockets = new ConcurrentHashMap<String, IPCServerSocket>();
        
        // Main data directory setup
        
        final File dataDirectory = this.getDataFolder();
        try {
            if (!dataDirectory.exists()) {
                if (!dataDirectory.mkdirs()) {
                    this.logger.log(Level.WARNING, "BungeeIPC data directory not created at " + dataDirectory.getPath());
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    return;
                }
            } else if (!dataDirectory.isDirectory()) {
                this.logger.log(Level.WARNING, "BungeeIPC data directory is not a directory: " + dataDirectory.getPath());
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                return;
            }
        } catch (SecurityException e) {
            this.logger.log(Level.WARNING, "Unable to validate if the BungeeIPC data directory has been properly created at " + dataDirectory.getPath());
            this.logger.log(Level.WARNING, "IPC Client will not be started.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            return;
        }
        
        // Configuration (re-)loading
        //     This includes all IPCServerSocket setup, as well as other
        //     miscellaneous configuration items.
        
        this.reloadConfig(this.getProxy().getConsole(), false);
    }
    
    /**
     * Disables this {@link BungeeIPCPlugin}.
     */
    @Override
    public void onDisable() {
        this.removeReader("PROXY_COMMAND");
        this.serverStatusUpdater.stop();
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.stop();
        }
    }
    
    /////////////////////////////////////
    // IPC SERVER PLUGIN FUNCTIONALITY //
    /////////////////////////////////////
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader) {
        this.validateNotBlank(channel, "Channel cannot be blank!");
        if (this.readers.containsKey(channel)) {
            return false;
        }
        return this.readers.put(channel, reader) == null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeReader(@NotNull final String channel) {
        this.validateNotBlank(channel, "Channel cannot be blank!");
        return this.readers.remove(channel) != null;
    }
    
    /**
     * {@inheritDoc}
     */
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
    
    /**
     * {@inheritDoc}
     */
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegisteredServer(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank.");
        return this.serverSockets.containsKey(name);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isServerRunning(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank.");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy.");
        return this.serverSockets.get(name).isRunning();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isServerConnected(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank.");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy.");
        return this.serverSockets.get(name).isConnected();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void restartServer(@NotNull final String name) {
        this.validateNotBlank(name, "Server name cannot be blank.");
        this.validateServer(name, "Server is not registered to the BungeeCord proxy.");
        this.serverSockets.get(name).stop();
        this.scheduler.schedule(this, () -> this.serverSockets.get(name).start(), 2L, TimeUnit.SECONDS);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastMessage(@NotNull final IPCMessage message) {
        
        if (!message.getChannel().equals(IPCMessage.BROADCAST_SERVER)) {
            this.logger.log(Level.WARNING, "Cannot broadcast IPC message when the server is not the broadcast server.");
            this.logger.log(Level.WARNING, "IPC message server: " + message.getServer());
            return;
        }
        
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.sendMessage(message);
        }
    }
    
    /**
     * Validates that the given {@link String value} is not empty (or only
     * whitespace).
     *
     * @param value The {@link String value} to check for being blank.
     * @param message The error message to display if the value is blank.
     * @throws IllegalArgumentException If the given value is blank.
     */
    private void validateNotBlank(@NotNull final String value, @NotNull final String message) throws IllegalArgumentException {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Validates that the given server name is a registered
     * {@link IPCServerSocket}.
     * 
     * @param name The name of the {@link IPCServerSocket} to validate.
     * @param message The error message to display if an {@link IPCServerSocket}
     *                with the given name does not exist.
     * @throws IllegalArgumentException If the given name is not a registered
     *                                  {@link IPCServerSocket}.
     */
    private void validateServer(@NotNull final String name, @NotNull final String message) throws IllegalArgumentException {
        if (!this.serverSockets.containsKey(name)) {
            throw new IllegalArgumentException(message);
        }
    }
    
    //////////////////////////////
    // GENERAL ATTRIBUTE ACCESS //
    //////////////////////////////
    
    /**
     * Gets if extra logging (debug logging) is enabled or not.
     * 
     * @return {@code true} if extra logging (debug logging) is enabled,
     *         {@code false} otherwise.
     */
    public boolean isExtraLoggingEnabled() {
        return this.extraLogging.get();
    }
    
    ////////////////////////////
    // COMMAND PUBLIC METHODS //
    ////////////////////////////
    
    /**
     * Sets the online status for the {@link ServerInfo Server} with the given
     * name.
     * 
     * @param name The name of the {@link ServerInfo Server} to change the
     *             online status of.
     * @param online {@code true} if the {@link ServerInfo Server} is online,
     *               {@code false} otherwise.
     */
    synchronized void setOnlineStatus(@NotNull final String name, final boolean online) {
        this.onlineStatuses.get(name).set(online);
    }
    
    /**
     * Gets the online status for the {@link ServerInfo Server} with the given
     * name.
     * 
     * @param name The name of the {@link ServerInfo Server} to retrieve the
     *             online status of.
     * @return {@code -1} if the {@link ServerInfo Server} is not registered or
     *         otherwise does not exist, {@code 0} if it is offline or otherwise
     *         disconnected, or {@code 1} if it is online.
     */
    public int getOnlineStatus(@NotNull final String name) {
        if (!this.onlineStatuses.containsKey(name)) {
            return -1;
        } else if (!this.onlineStatuses.get(name).get()) {
            return 0;
        } else {
            return 1;
        }
    }
    
    /**
     * Reloads the configuration file, displaying any error messages to the
     * given {@link CommandSender}.
     *
     * @param sender The {@link CommandSender} that triggered the configuration
     *               reload.
     */
    public void reloadConfig(@NotNull final CommandSender sender) {
        this.reloadConfig(sender, true);
    }
    
    /**
     * Reloads the configuration file, displaying any error messages to the
     * given {@link CommandSender} if this was triggered via {@link Command}.
     *
     * @param sender The {@link CommandSender} that triggered the configuration
     *               reload.
     * @param command If {@code true}, error messages will be displayed to the
     *                {@link CommandSender} if any error occurs. If
     *                {@code false}, errors will only be shown in the console
     *                logs.
     */
    private void reloadConfig(@NotNull final CommandSender sender, final boolean command) {
        
        for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
            serverSocket.stop();
        }
        this.serverSockets.clear();
        
        this.scheduler.runAsync(this, () -> {
        
            final ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
            
            File configFile = new File(this.getDataFolder(), "bungeeipc.yml");
            try {
                if (!configFile.exists() || !configFile.isFile()) {
                    configFile = new File(this.getDataFolder(), "config.yml");
                }
                
                if (configFile.exists()) {
                    if (!configFile.isFile()) {
                        this.logger.log(Level.WARNING, "BungeeIPC configuration file is not a file: " + configFile.getPath());
                        this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                        if (command) {
                            sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                        }
                        return;
                    }
                } else {
                    if (!configFile.createNewFile()) {
                        this.logger.log(Level.WARNING, "BungeeIPC configuration file not created at " + configFile.getPath());
                        this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                        if (command) {
                            sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                        }
                        return;
                    }
                
                    final InputStream defaultConfig = this.getResourceAsStream(configFile.getName());
                    final FileOutputStream outputStream = new FileOutputStream(configFile);
                    final byte[] buffer = new byte[4096];
                    int bytesRead;
                
                    while ((bytesRead = defaultConfig.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                
                    outputStream.flush();
                    outputStream.close();
                    
                    this.logger.log(Level.WARNING, "The BungeeIPC configuration file did not exist at " + configFile.getPath());
                    this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                    this.logger.log(Level.WARNING, "Please update the configuration file as required for your installation, and then run \"/ipcb reload\".");
                    if (command) {
                        final ComponentBuilder builder1 = new ComponentBuilder("The BungeeIPC configuration file did not exist; a copy of the default has been made and placed in the correct location.").color(ChatColor.RED);
                        final ComponentBuilder builder2 = new ComponentBuilder("Please update the configuration as required for the installation, and then run ").color(ChatColor.RED);
                        builder2.append("/ipcb reload").color(ChatColor.AQUA);
                        builder2.append(".").color(ChatColor.RED);
                        sender.sendMessage(builder1.create());
                        sender.sendMessage(builder2.create());
                    }
                    return;
                }
            } catch (SecurityException | IOException e) {
                this.logger.log(Level.WARNING, "Unable to load the BungeeIPC configuration file at " + configFile.getPath());
                this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                }
                return;
            }
        
            final Configuration config;
            try {
                config = provider.load(configFile);
            } catch (IOException e) {
                this.logger.log(Level.WARNING, "Unable to load the BungeeIPC configuration.");
                this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                }
                return;
            }
            if (config == null) {
                this.logger.log(Level.WARNING, "BungeeIPC configuration not loaded, no Exception thrown.");
                this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                if (command) {
                    sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                }
                return;
            }
            
            this.extraLogging.set(config.getBoolean("extra_logging", false));
            
            final SSLServerSocketFactory sslServerSocketFactory;
            final List<String> tlsVersionWhitelist = new ArrayList<String>();
            final List<String> tlsCipherSuiteWhitelist = new ArrayList<String>();
            
            if (!config.getBoolean("use_ssl", false)) {
                sslServerSocketFactory = null;
            } else {
        
                final String keyStoreFile = config.getString("key_store_file", null);
                final String keyStorePassword = config.getString("key_store_password", null);
        
                if (keyStoreFile == null || keyStoreFile.trim().isEmpty()) {
                    this.logger.log(Level.WARNING, "KeyStore file is null or empty: " + (keyStoreFile == null ? "null" : keyStoreFile));
                    this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                    if (command) {
                        final ComponentBuilder builder1 = new ComponentBuilder("Please check the BungeeIPC configuration file, specifically the \"key_store_file\" item, to verify it has the correct value.").color(ChatColor.RED);
                        final ComponentBuilder builder2 = new ComponentBuilder("After confirming the value, please run ").color(ChatColor.RED);
                        builder2.append("/ipcb reload").color(ChatColor.AQUA);
                        builder2.append(" to reload the new value.").color(ChatColor.RED);
                        sender.sendMessage(builder1.create());
                        sender.sendMessage(builder2.create());
                    }
                    return;
                }
        
                if (keyStorePassword == null || keyStorePassword.trim().isEmpty()) {
                    this.logger.log(Level.WARNING, "KeyStore password is null or empty: " + (keyStorePassword == null ? "null" : keyStorePassword));
                    this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                    if (command) {
                        final ComponentBuilder builder1 = new ComponentBuilder("Please check the BungeeIPC configuration file, specifically the \"key_store_password\" item, to verify it has the correct value.").color(ChatColor.RED);
                        final ComponentBuilder builder2 = new ComponentBuilder("After confirming the value, please run ").color(ChatColor.RED);
                        builder2.append("/ipcb reload").color(ChatColor.AQUA);
                        builder2.append(" to reload the new value.").color(ChatColor.RED);
                        sender.sendMessage(builder1.create());
                        sender.sendMessage(builder2.create());
                    }
                    return;
                }
        
                String keyStoreInstance = config.getString("key_store_instance", "JKS");
                if (keyStoreInstance == null || keyStoreInstance.trim().isEmpty()) {
                    keyStoreInstance = "JKS";
                }
                String keyManagerFactoryAlgorithm = config.getString("key_manager_factory_algorithm", "NewSunX509");
                if (keyManagerFactoryAlgorithm == null || keyManagerFactoryAlgorithm.trim().isEmpty()) {
                    keyManagerFactoryAlgorithm = "NewSunX509";
                }
                String trustManagerFactoryAlgorithm = config.getString("trust_manager_factory_algorithm", "SunX509");
                if (trustManagerFactoryAlgorithm == null || trustManagerFactoryAlgorithm.trim().isEmpty()) {
                    trustManagerFactoryAlgorithm = "SunX509";
                }
                String sslContextProtocol = config.getString("ssl_context_protocol", "TLS");
                if (sslContextProtocol == null || sslContextProtocol.trim().isEmpty()) {
                    sslContextProtocol = "TLS";
                }
        
                final List<String> tlsVersionWhitelistRaw = config.getStringList("tls_version_whitelist");
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
        
                final List<String> tlsCipherSuiteWhitelistRaw = config.getStringList("tls_cipher_suite_whitelist");
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
                    this.logger.log(Level.WARNING, "Unable to create SSLServerSocketFactory.");
                    this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    if (command) {
                        sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                    }
                    return;
                }
            }
            
            final Configuration serversConfig = config.getSection("servers");
            if (serversConfig == null || serversConfig.getKeys().isEmpty()) {
                this.logger.log(Level.INFO, "BungeeIPC configuration file has been reloaded.");
                this.logger.log(Level.INFO, "No IPC Servers defined, nothing to start.");
                if (command) {
                    final ComponentBuilder builder1 = new ComponentBuilder("The BungeeIPC configuration has been reloaded successfully.").color(ChatColor.GREEN);
                    final ComponentBuilder builder2 = new ComponentBuilder("There are no IPC Servers configured. If this is correct, you can ignore this message.").color(ChatColor.GOLD);
                    final ComponentBuilder builder3 = new ComponentBuilder("If this is not correct, please update the configuration as required, and then run ").color(ChatColor.GOLD);
                    builder3.append("/ipcb reload").color(ChatColor.AQUA);
                    builder3.append(" to reload the updated configuration.").color(ChatColor.GOLD);
                    sender.sendMessage(builder1.create());
                    sender.sendMessage(builder2.create());
                    sender.sendMessage(builder3.create());
                }
                return;
            }
            
            final HashSet<String> connections = new HashSet<String>();
            final Collection<InetAddress> localAddresses = new ArrayList<InetAddress>();
            
            for (final ListenerInfo listenerInfo : this.getProxy().getConfig().getListeners()) {
                final SocketAddress address = listenerInfo.getSocketAddress();
                if (address == null) {
                    continue;
                }
                if (!(address instanceof InetSocketAddress)) {
                    continue;
                }
                
                final InetSocketAddress hostAddress = (InetSocketAddress) address;
                connections.add(hostAddress.getAddress().getHostAddress() + ":" + hostAddress.getPort());
            }
            
        
            try {
                for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    localAddresses.addAll(Collections.list(iface.getInetAddresses()));
                }
            } catch (SocketException e) {
                this.logger.log(Level.WARNING, "Unable to load all local network interfaces.");
                this.logger.log(Level.WARNING, "None of the IPC Servers will be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage(new ComponentBuilder("An error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.").color(ChatColor.RED).create());
                }
                return;
            }
            
            this.scheduler.runAsync(this, () -> {
                
                for (final String serverName : serversConfig.getKeys()) {
        
                    final Configuration serverConfig = serversConfig.getSection(serverName);
                    final BungeeIPCServerSocket serverSocket;
                    try {
                        serverSocket = new BungeeIPCServerSocket(this, serverName, serverConfig, localAddresses, sslServerSocketFactory, tlsVersionWhitelist, tlsCipherSuiteWhitelist);
                    } catch (IllegalArgumentException e) {
                        this.logger.log(Level.WARNING, "Failure while attempting to create IPCServerSocket " + serverName + ".");
                        this.logger.log(Level.WARNING, "IPC Server " + serverName + " will not be started.");
                        this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                        if (command) {
                            final ComponentBuilder builder1 = new ComponentBuilder("An error has occurred while (re)loading one of the IPC Servers. Please check the BungeeIPC configuration section for the IPC Server ").color(ChatColor.RED);
                            builder1.append(serverName).color(ChatColor.AQUA);
                            builder1.append(".").color(ChatColor.RED);
                            final ComponentBuilder builder2 = new ComponentBuilder("After updating the configuration section as needed, please run ").color(ChatColor.GOLD);
                            builder2.append("/ipcb reload").color(ChatColor.AQUA);
                            builder2.append(" to reload the updated configuration.").color(ChatColor.GOLD);
                            final ComponentBuilder builder3 = new ComponentBuilder("If you believe the configuration has no issues, or this error persists after updating and reloading, please report it to a server administrator.").color(ChatColor.RED);
                            sender.sendMessage(builder1.create());
                            sender.sendMessage(builder2.create());
                            sender.sendMessage(builder3.create());
                        }
                        continue;
                    }
        
                    final String connection = serverSocket.getAddress().getHostAddress() + ":" + serverSocket.getPort();
                    if (!connections.add(connection)) {
                        this.logger.log(Level.WARNING, "Non-unique IPC connection.");
                        this.logger.log(Level.WARNING, "IPCServerSocket name: " + serverName);
                        this.logger.log(Level.WARNING, "Hostname/IP Address: " + serverSocket.getAddress().getHostAddress());
                        this.logger.log(Level.WARNING, "Port: " + serverSocket.getPort());
                        this.logger.log(Level.WARNING, "IPC Server " + serverName + " will not be started.");
                        if (command) {
                            final ComponentBuilder builder1 = new ComponentBuilder("An error has occurred while (re)loading one of the IPC Servers. Please check the BungeeIPC configuration section for the IPC Server ").color(ChatColor.RED);
                            builder1.append(serverName).color(ChatColor.AQUA);
                            builder1.append(".").color(ChatColor.RED);
                            final ComponentBuilder builder2 = new ComponentBuilder("This appears to be an issue with ").color(ChatColor.RED);
                            builder2.append("non-unique connection information (hostname/IP address and port combination are used somewhere else)").color(ChatColor.AQUA);
                            builder2.append(". Please update the BungeeIPC configuration to remove this conflict.").color(ChatColor.RED);
                            final ComponentBuilder builder3 = new ComponentBuilder("After updating the configuration section as needed, please run ").color(ChatColor.GOLD);
                            builder3.append("/ipcb reload").color(ChatColor.AQUA);
                            builder3.append(" to reload the updated configuration.").color(ChatColor.GOLD);
                            final ComponentBuilder builder4 = new ComponentBuilder("If you believe the configuration has no issues, or this error persists after updating and reloading, please report it to a server administrator.").color(ChatColor.RED);
                            sender.sendMessage(builder1.create());
                            sender.sendMessage(builder2.create());
                            sender.sendMessage(builder3.create());
                            sender.sendMessage(builder4.create());
                        }
                        continue;
                    }
        
                    if (this.serverSockets.containsKey(serverName)) {
                        this.logger.log(Level.WARNING, "IPCServerSocket previously defined and added.");
                        this.logger.log(Level.WARNING, "Please check all configurations for duplicates.");
                        this.logger.log(Level.WARNING, "Duplicate IPCServerSocket name: " + serverName);
                        this.logger.log(Level.WARNING, "IPC Server " + serverName + " will not be started.");
                        if (command) {
                            final ComponentBuilder builder1 = new ComponentBuilder("An error has occurred while (re)loading one of the IPC Servers. Please check the BungeeIPC configuration section for the IPC Server ").color(ChatColor.RED);
                            builder1.append(serverName).color(ChatColor.AQUA);
                            builder1.append(".").color(ChatColor.RED);
                            final ComponentBuilder builder2 = new ComponentBuilder("This appears to be an issue with ").color(ChatColor.RED);
                            builder2.append("non-unique IPC Server names").color(ChatColor.AQUA);
                            builder2.append(". Please update the BungeeIPC configuration to remove this conflict.").color(ChatColor.RED);
                            final ComponentBuilder builder3 = new ComponentBuilder("After updating the configuration section as needed, please run ").color(ChatColor.GOLD);
                            builder3.append("/ipcb reload").color(ChatColor.AQUA);
                            builder3.append(" to reload the updated configuration.").color(ChatColor.GOLD);
                            final ComponentBuilder builder4 = new ComponentBuilder("If you believe the configuration has no issues, or this error persists after updating and reloading, please report it to a server administrator.").color(ChatColor.RED);
                            sender.sendMessage(builder1.create());
                            sender.sendMessage(builder2.create());
                            sender.sendMessage(builder3.create());
                            sender.sendMessage(builder4.create());
                        }
                        continue;
                    }
        
                    this.serverSockets.put(serverName, serverSocket);
                }
    
                for (final IPCServerSocket serverSocket : this.serverSockets.values()) {
                    serverSocket.start();
                }
                
                if (command) {
                    final ComponentBuilder builder = new ComponentBuilder("The BungeeIPC configuration has been reloaded. Please run ").color(ChatColor.GREEN);
                    builder.append("/ipcb status").color(ChatColor.AQUA);
                    builder.append(" in a few seconds to verify that the IPC Servers have reloaded and reconnected successfully.").color(ChatColor.GREEN);
                    sender.sendMessage(builder.create());
                }
            });
        });
    }
}

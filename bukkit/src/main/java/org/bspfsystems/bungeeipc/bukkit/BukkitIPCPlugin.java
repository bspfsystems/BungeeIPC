/* 
 * This file is part of the BungeeIPC plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
 * 
 * Copyright (C) 2020-2022 BSPF Systems, LLC (https://bspfsystems.org/)
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.bspfsystems.bungeeipc.api.client.ClientIPCSocket;
import org.bspfsystems.bungeeipc.api.common.IPCMessage;
import org.bspfsystems.bungeeipc.api.common.IPCReader;
import org.bspfsystems.bungeeipc.api.client.ClientIPCPlugin;
import org.bspfsystems.bungeeipc.bukkit.command.IPCTabExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the implementation of a Bukkit {@link JavaPlugin} that provides
 * the {@link ClientIPCPlugin} functionality.
 */
public final class BukkitIPCPlugin extends JavaPlugin implements ClientIPCPlugin {
    
    private Logger logger;
    
    private BukkitScheduler scheduler;
    
    private ClientIPCSocket socket;
    private ConcurrentHashMap<String, IPCReader> readers;
    
    /**
     * Explicitly define the constructor.
     */
    public BukkitIPCPlugin() {
        super();
    }
    
    /**
     * Enables this {@link BukkitIPCPlugin}.
     */
    @Override
    public void onEnable() {
        
        this.logger = this.getLogger();
        
        this.logger.log(Level.INFO, "///////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "//                                                                       //");
        this.logger.log(Level.INFO, "// BungeeIPC Bukkit/BungeeCord plugin for Minecraft                      //");
        this.logger.log(Level.INFO, "// Copyright (C) 2020-2022 BSPF Systems, LLC (https://bspfsystems.org/)  //");
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
        
        // IPCReader setup
        
        this.readers = new ConcurrentHashMap<String, IPCReader>();
        this.addReader("SERVER_COMMAND", new BungeeBukkitIPCReader(this));
        
        // Command setup
        
        this.registerCommand("ipc", new IPCTabExecutor(this));
        
        // Main data directory setup
        
        final File dataDirectory = this.getDataFolder();
        try {
            if (!dataDirectory.exists()) {
                if (!dataDirectory.mkdirs()) {
                    this.logger.log(Level.SEVERE, "BungeeIPC data directory not created at " + dataDirectory.getPath());
                    this.logger.log(Level.SEVERE, "IPC Client will not be started.");
                    return;
                }
            } else if (!dataDirectory.isDirectory()) {
                this.logger.log(Level.SEVERE, "BungeeIPC data directory is not a directory: " + dataDirectory.getPath());
                this.logger.log(Level.SEVERE, "IPC Client will not be started.");
                return;
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Unable to validate if the BungeeIPC data directory has been properly created at " + dataDirectory.getPath());
            this.logger.log(Level.SEVERE, "IPC Client will not be started.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            return;
        }
        
        // Configuration (re-)loading
        //     This includes all IPCClientSocket setup, as well as other
        //     miscellaneous configuration items.
        
        this.reloadConfig(this.getServer().getConsoleSender(), false);
    }
    
    /**
     * Registers the {@link PluginCommand} with the given name to the given
     * {@link TabExecutor}.
     * <p>
     * If no {@link PluginCommand} is found, an error will be logged an a
     * {@link RuntimeException} will be thrown.
     * 
     * @param commandName The name of the {@link PluginCommand} to retrieve.
     * @param tabExecutor The {@link TabExecutor} to register to the
     *                    {@link PluginCommand}.
     * @throws RuntimeException If no {@link PluginCommand} with the given name
     *                          can be found.
     */
    private void registerCommand(@NotNull final String commandName, @NotNull final TabExecutor tabExecutor) throws RuntimeException {
        final PluginCommand command = this.getCommand(commandName);
        if (command == null) {
            this.logger.log(Level.SEVERE, "Cannot find the /" + commandName + " command.");
            this.logger.log(Level.SEVERE, "IPC Client will not be started.");
            throw new RuntimeException("Cannot find the /" + commandName + " command.");
        }
        command.setExecutor(tabExecutor);
        command.setTabCompleter(tabExecutor);
    }
    
    
    /**
     * Disables this {@link BukkitIPCPlugin}.
     */
    @Override
    public void onDisable() {
        this.removeReader("SERVER_COMMAND");
        
        if (this.socket != null) {
            this.socket.stop();
        }
    }
    
    /////////////////////////////////////
    // IPC CLIENT PLUGIN FUNCTIONALITY //
    /////////////////////////////////////
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addReader(@NotNull final String channel, @NotNull final IPCReader reader) {
        BukkitIPCPlugin.validateNotBlank(channel, "Channel cannot be blank!");
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
        BukkitIPCPlugin.validateNotBlank(channel, "Channel cannot be blank!");
        return this.readers.remove(channel) != null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(@NotNull final IPCMessage message) {
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Unable to send IPC message.");
            this.logger.log(Level.WARNING, "IPC Client not configured.");
            return;
        }
        this.socket.sendMessage(message);
    }
    
    /**
     * {@inheritDoc}
     */
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientRunning() {
        return this.socket != null && this.socket.isRunning();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientConnected() {
        return this.socket != null && this.socket.isConnected();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void restartClient() {
        
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Cannot restart client, IPCSocket is null.");
            return;
        }
        this.socket.stop();
        
        
        this.scheduler.runTaskLaterAsynchronously(this, this::startClient, 40);
    }
    
    /**
     * Performs the starting of the {@link ClientIPCSocket}.
     */
    private void startClient() {
        if (this.socket == null) {
            this.logger.log(Level.WARNING, "Cannot start client, IPCSocket is null.");
            return;
        }
        this.socket.start();
    }
    
    /**
     * Validates that the given {@link String value} is not empty (or only
     * whitespace).
     *
     * @param value The {@link String value} to check for being blank.
     * @param message The error message to display if the value is blank.
     * @throws IllegalArgumentException If the given value is blank.
     */
    private static void validateNotBlank(@NotNull final String value, @NotNull final String message) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    ////////////////////////////
    // COMMAND PUBLIC METHODS //
    ////////////////////////////
    
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
        
        if (this.socket != null) {
            this.socket.stop();
        }
        this.socket = null;
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            
            File configFile = new File(this.getDataFolder(), "bukkitipc.yml");
            try {
                
                if (!configFile.exists() || !configFile.isFile()) {
                    configFile = new File(this.getDataFolder(), "config.yml");
                }
                
                if (configFile.exists()) {
                    if (!configFile.isFile()) {
                        this.logger.log(Level.WARNING, "BungeeIPC configuration file is not a file: " + configFile.getPath());
                        this.logger.log(Level.WARNING, "IPC Client will not be started.");
                        if (command) {
                            sender.sendMessage("§r§cAn error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                        }
                        return;
                    }
                } else {
                    if (!configFile.createNewFile()) {
                        this.logger.log(Level.WARNING, "BungeeIPC configuration file not created at " + configFile.getPath());
                        this.logger.log(Level.WARNING, "IPC Client will not be started.");
                        if (command) {
                            sender.sendMessage("§r§cAn error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                        }
                        return;
                    }
                    
                    final InputStream defaultConfig = this.getResource(configFile.getName());
                    final FileOutputStream outputStream = new FileOutputStream(configFile);
                    final byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = defaultConfig.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    outputStream.flush();
                    outputStream.close();
                    
                    this.logger.log(Level.WARNING, "BungeeIPC configuration file did not exist at " + configFile.getPath());
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    this.logger.log(Level.WARNING, "Please update the configuration as required for your installation, and then run \"/ipc reload\".");
                    if (command) {
                        sender.sendMessage("§r§cThe BungeeIPC configuration file did not exist; a copy of the default has been made and placed in the correct location.§r");
                        sender.sendMessage("§r§cPlease update the configuration as required for the installation, and then run§r §b/ipc reload§r§c.§r");
                    }
                    return;
                }
            } catch (SecurityException | IOException e) {
                this.logger.log(Level.WARNING, "Unable to load the BungeeIPC configuration file at " + configFile.getPath());
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage("§r§cAn error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                }
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(configFile);
            } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load BungeeIPC configuration.");
                this.logger.log(Level.WARNING, "IPC Client will not be started.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage("§r§cAn error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                }
                return;
            }
            
            final Level loggingLevel;
            Level rawLoggingLevel;
            try {
                rawLoggingLevel = Level.parse(config.getString("logging_level", "INFO"));
            } catch (NullPointerException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load the BungeeIPC logging level.");
                this.logger.log(Level.WARNING, "Will use the default level (INFO).");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                if (command) {
                    sender.sendMessage("§r§cAn error has occurred while (re)loading the BungeeIPC configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                }
                rawLoggingLevel = Level.INFO;
            }
            loggingLevel = rawLoggingLevel;
            
            final SSLSocketFactory sslSocketFactory;
            final ArrayList<String> tlsVersionWhitelist = new ArrayList<String>();
            final ArrayList<String> tlsCipherSuiteWhitelist = new ArrayList<String>();
            
            if (!config.getBoolean("use_ssl", false)) {
                sslSocketFactory = null;
            } else {
                
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
                    final SSLContext sslContext = SSLContext.getInstance(sslContextProtocol);
                    sslContext.init(null, null, null);
                    
                    sslSocketFactory = sslContext.getSocketFactory();
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    this.logger.log(Level.WARNING, "Unable to create SSLSocketFactory.");
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + "thrown.", e);
                    if (command) {
                        sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                    }
                    return;
                }
            }
            
            this.scheduler.runTask(this, () -> {
                
                try {
                    this.socket = new BukkitClientIPCSocket(this, config, sslSocketFactory, tlsVersionWhitelist, tlsCipherSuiteWhitelist);
                } catch (IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "Unable to create IPC Client.");
                    this.logger.log(Level.WARNING, "IPC Client will not be started.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    if (command) {
                        sender.sendMessage("§r§cAn error has occurred while (re)loading the IPC Client configuration. Please try again. If this error persists, please report it to a server administrator.§r");
                    }
                    this.socket = null;
                    return;
                }
                
                this.logger.setLevel(loggingLevel);
                this.socket.start();
                if (command) {
                    sender.sendMessage("§r§aThe BungeeIPC configuration has been reloaded. Please run§r §b/ipc status§r §ain a few seconds to verify that the IPC Client has reloaded and reconnected successfully.§r");
                }
            });
        });
    }
}

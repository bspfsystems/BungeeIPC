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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.bspfsystems.bungeeipc.api.IPCMessage;
import org.bspfsystems.bungeeipc.api.IPCReader;
import org.bspfsystems.bungeeipc.api.plugin.IPCClientPlugin;
import org.bspfsystems.bungeeipc.bukkit.command.IPCTabExecutor;
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
                
                final InputStream defaultConfig = this.getResource(mainConfigFile.getName());
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
        
        final YamlConfiguration mainConfig = new YamlConfiguration();
        try {
            mainConfig.load(mainConfigFile);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            this.logger.log(Level.SEVERE, "Unable to load the main BungeeIPC configuration.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to load the main BungeeIPC configuration.", e);
        }
        
        SSLSocketFactory sslSocketFactory = null;
        ArrayList<String> tlsVersionWhitelist = null;
        ArrayList<String> tlsCipherSuiteWhitelist = null;
        
        final boolean useSSL = mainConfig.getBoolean("use_ssl", false);
        if (useSSL) {
            
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
                final SSLContext sslContext = SSLContext.getInstance(sslContextProtocol);
                sslContext.init(null, null, null);
                
                sslSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                this.logger.log(Level.SEVERE, "Unable to create SSLSocketFactory.");
                this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + "thrown.", e);
                throw new RuntimeException("Unable to create SSLSocketFactory.", e);
            }
        }
        
        final File clientConfigDirectory = new File(this.getDataFolder(), "IPC_Client");
        try {
            if (!clientConfigDirectory.exists()) {
                this.logger.log(Level.SEVERE, "IPC Client configuration directory does not exist at " + clientConfigDirectory.getPath());
                throw new RuntimeException("IPC Client configuration directory does not exist at " + clientConfigDirectory.getPath());
            } else if (!clientConfigDirectory.isDirectory()) {
                this.logger.log(Level.SEVERE, "IPC Client configuration directory is not a directory: " + clientConfigDirectory.getPath());
                throw new RuntimeException("IPC Client configuration directory is not a directory: " + clientConfigDirectory.getPath());
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Cannot validate existence of IPC Client configuration directory at " + clientConfigDirectory.getPath());
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Cannot validate existence of IPC Client configuration directory at " + clientConfigDirectory.getPath(), e);
        }
        
        final File clientConfigFile = new File(clientConfigDirectory, "ipc_client.yml");
        try {
            if (!clientConfigFile.exists()) {
                this.logger.log(Level.SEVERE, "IPC Client configuration file does not exist at " + clientConfigFile.getPath());
                throw new RuntimeException("IPC Client configuration file does not exist at " + clientConfigFile.getPath());
            } else if (!clientConfigFile.isFile()) {
                this.logger.log(Level.SEVERE, "IPC Client configuration file is not a file: " + clientConfigFile.getPath());
                throw new RuntimeException("IPC Client configuration file is not a file: " + clientConfigFile.getPath());
            }
        } catch (SecurityException e) {
            this.logger.log(Level.SEVERE, "Cannot validate existence of IPC Client configuration file at " + clientConfigFile.getPath());
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Cannot validate existence of IPC Client configuration file at " + clientConfigFile.getPath(), e);
        }
        
        final YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(clientConfigFile);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            this.logger.log(Level.SEVERE, "Unable to load IPC Client configuration.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to load IPC Client configuration.", e);
        }
        
        try {
            this.socket = new BukkitIPCSocket(this, config, sslSocketFactory, tlsVersionWhitelist, tlsCipherSuiteWhitelist);
        } catch (IllegalArgumentException e) {
            this.logger.log(Level.SEVERE, "Unable to create IPC Client.");
            this.logger.log(Level.SEVERE, e.getClass().getSimpleName() + " thrown.", e);
            throw new RuntimeException("Unable to create an IPC Client.", e);
        }
        
        this.socket.start();
        
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
